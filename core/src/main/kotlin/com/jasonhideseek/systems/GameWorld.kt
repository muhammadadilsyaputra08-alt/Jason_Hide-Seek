package com.jasonhideseek.systems

import com.jasonhideseek.ai.JasonDecisionEngine
import com.jasonhideseek.config.Dir
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.entities.JasonEntity
import com.jasonhideseek.entities.JasonState
import com.jasonhideseek.entities.Player
import com.jasonhideseek.entities.Survivor
import com.jasonhideseek.entities.SurvivorAction
import com.jasonhideseek.entities.SurvivorBot
import com.jasonhideseek.entities.SurvivorStatus
import com.jasonhideseek.game.JasonGame
import com.jasonhideseek.ui.TouchControls
import com.jasonhideseek.world.ExitOrientation
import com.jasonhideseek.world.ExitState
import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.KeyFrame
import com.jasonhideseek.world.MapRepository
import com.jasonhideseek.world.SpotState
import java.util.Random

enum class Phase { COUNTDOWN, ESCAPE, ENDED }

/** Tombol aksi kontekstual pemain. */
enum class ContextAction { NONE, HIDE, LEAVE, EXIT }

/** Simulasi satu pertandingan: fase, survivor, Key Frame, pintu keluar, spectator, dan hasil akhir. */
class GameWorld(game: JasonGame, mapIndex: Int) {
    val map = MapRepository.load(mapIndex)
    val rng = Random()
    val sound = game.sound
    val gore = GoreAndDebrisSystem(game.assets, rng)
    val vision = VisionSystem(map)
    val spots = ArrayList<HidingSpot>()
    val bots = ArrayList<SurvivorBot>()
    val survivors = ArrayList<Survivor>()
    val keys = ArrayList<KeyFrame>()
    val player = Player(map.playerSpawn.x, map.playerSpawn.y)
    val jason = JasonEntity(map.jasonSpawn.x, map.jasonSpawn.y, JasonDecisionEngine(rng), rng)

    var phase = Phase.COUNTDOWN
        private set
    var phaseRemaining = GameConfig.COUNTDOWN_TIME
        private set
    var result: MatchResult? = null
        private set
    var endTimer = 0f
        private set
    var elapsed = 0f
        private set
    var tension = 0f
        private set

    var keysCollected = 0
        private set
    var exitState = ExitState.LOCKED
        private set
    var exitAnimTime = 0f
        private set
    var toast: String? = null
        private set
    var toastTimer = 0f
        private set

    /** Bot yang sedang diamati saat pemain tereliminasi. */
    var spectateTarget: Survivor? = null
        private set

    val keyComplete get() = keysCollected >= GameConfig.KEY_FRAME_COUNT
    val escapePhase get() = phase == Phase.ESCAPE
    val escapeProgress get() = if (phase == Phase.ESCAPE) 1f - phaseRemaining / GameConfig.ESCAPE_TIME else 0f
    val huntProgress get() = escapeProgress
    val survivorsAlive get() = survivors.count { it.alive }
    val escapedCount get() = survivors.count { it.status == SurvivorStatus.ESCAPED }
    val eliminatedCount get() = survivors.count { it.status == SurvivorStatus.ELIMINATED }
    val spectating get() = !player.alive && player.status == SurvivorStatus.ELIMINATED

    /** Survivor yang menjadi fokus kamera utama. */
    val focus: Survivor get() = if (spectating) (spectateTarget ?: player) else player

    init {
        for (d in map.spotDefs) spots.add(HidingSpot(d.type, d.x, d.y))
        survivors.add(player)

        // Spawn bot: tile lantai acak, jauh dari Jason, bukan titik sembunyi / spawn pemain
        val pool = map.floorTiles.filter { t ->
            Math.hypot((t.x - map.jasonSpawn.x).toDouble(), (t.y - map.jasonSpawn.y).toDouble()) > 200.0 &&
                Math.hypot((t.x - map.playerSpawn.x).toDouble(), (t.y - map.playerSpawn.y).toDouble()) > 24.0 &&
                spots.none { Math.hypot((t.x - it.x).toDouble(), (t.y - it.y).toDouble()) < 4.0 }
        }.toMutableList()
        pool.shuffle(rng)
        for (i in 0 until GameConfig.BOT_COUNT) {
            val t = pool[i % pool.size]
            val b = SurvivorBot("bot${i + 1}", t.x, t.y, rng)
            bots.add(b); survivors.add(b)
        }

        // 3 Key Frame acak dari kandidat yang terjangkau
        val cands = map.keyCandidates.toMutableList()
        cands.shuffle(rng)
        val scratch = ArrayList<com.badlogic.gdx.math.Vector2>()
        for (c in cands) {
            if (keys.size >= GameConfig.KEY_FRAME_COUNT) break
            if (!map.isSolidTile(c.col, c.row) &&
                map.findPath(map.playerSpawn.x, map.playerSpawn.y, c.x, c.y, scratch)) {
                keys.add(KeyFrame(keys.size + 1, c.x, c.y))
            }
        }

        sound.noiseListeners.clear()
        for (b in bots) sound.noiseListeners.add { nx, ny, r -> b.hear(nx, ny, r) }
    }

    // ---------- Loop ----------
    fun update(dt: Float, controls: TouchControls) {
        gore.update(dt)
        if (toastTimer > 0f) { toastTimer -= dt; if (toastTimer <= 0f) toast = null }
        for (s in spots) {
            if (s.recheckCooldown > 0f) s.recheckCooldown -= dt
            if (s.suspicion > 0f) s.suspicion = Math.max(0f, s.suspicion - GameConfig.SUSPICION_DECAY * dt)
        }
        if (exitState == ExitState.UNLOCKING) {
            exitAnimTime += dt
            if (exitAnimTime >= GameConfig.EXIT_UNLOCK_TIME) {
                exitState = ExitState.OPEN
                sound.playAt("door_open", map.exitCenter.x, map.exitCenter.y, 1f)
            }
        }
        if (phase == Phase.ENDED) {
            endTimer += dt
            jason.updateEnded(dt)
            return
        }

        elapsed += dt
        phaseRemaining -= dt
        if (phase == Phase.COUNTDOWN && phaseRemaining <= 0f) {
            phase = Phase.ESCAPE
            phaseRemaining = GameConfig.ESCAPE_TIME
            jason.release()
            sound.play("alert", 0.9f)
            showToast("Cari 3 Key Frame!")
        }

        val wantsAction = controls.consumeAction()
        if (wantsAction && player.alive && !player.busy) handlePlayerAction()
        player.update(dt, controls, map)
        for (b in bots) if (b.alive) b.update(dt, this)
        for (s in survivors) {
            if (!s.alive) continue
            val done = s.tickAction(dt)
            if (done != null) onActionDone(s, done)
        }
        jason.update(dt, this)

        if (phase == Phase.ESCAPE) checkKeyPickups()
        updateSpectator()

        val f = focus
        sound.listenerX = f.x; sound.listenerY = f.y
        updateTension()
        sound.updateHeartbeat(dt, tension)
        evaluateRound()
    }

    private fun updateTension() {
        tension = when {
            phase != Phase.ESCAPE || !player.alive -> 0f
            jason.state == JasonState.CHASE -> 1f
            else -> {
                val d = jason.dist(player.x, player.y)
                val base = (1f - d / 220f).coerceIn(0f, 1f) * 0.75f
                if (player.hidden && jason.targetSpot === player.hidingIn) Math.max(base, 0.9f) else base
            }
        }
    }

    // ---------- Hasil ----------
    private fun evaluateRound() {
        val botsEscaped = bots.count { it.status == SurvivorStatus.ESCAPED }
        val timeUp = phase == Phase.ESCAPE && phaseRemaining <= 0f
        val r = MatchRules.evaluate(player.status, botsEscaped, survivorsAlive, timeUp) ?: return
        result = r; phase = Phase.ENDED; endTimer = 0f
        jason.setEnded(r == MatchResult.JASON_WINS)
        sound.play(if (r == MatchResult.JASON_WINS || r == MatchResult.TIMES_UP) "lose" else "win", 0.9f)
    }

    fun summary() = MatchSummary(
        result ?: MatchResult.TIMES_UP, map.name, player.status,
        escapedCount, eliminatedCount, survivors.size, elapsed.toInt(), keysCollected
    )

    // ---------- Key Frame ----------
    private fun checkKeyPickups() {
        for (k in keys) {
            if (k.collected) continue
            for (s in survivors) {
                if (!s.alive || s.hidden || s.busy) continue
                if (s.dist(k.x, k.y) <= GameConfig.KEY_PICKUP_RANGE) { collectKey(k, s); break }
            }
        }
    }

    private fun collectKey(k: KeyFrame, s: Survivor) {
        k.collected = true
        keysCollected++
        s.startAction(SurvivorAction.KEY_PICKUP, GameConfig.KEY_PICKUP_TIME)
        gore.animFx("fx_key_pickup", k.x, k.y + 8f, 14f)
        sound.playAt("key_pickup", k.x, k.y, 1f)
        if (keyComplete) {
            gore.animFx("fx_key_assembly", s.x, s.y + 22f, 10f)
            sound.play("key_assembly", 1f)
            showToast("COMPLETE KEY! Menuju pintu keluar")
        } else {
            showToast("Key Frame $keysCollected/${GameConfig.KEY_FRAME_COUNT}")
        }
    }

    private fun showToast(t: String) { toast = t; toastTimer = 2.5f }

    // ---------- Pintu keluar ----------
    fun canUseExit(s: Survivor): Boolean =
        phase == Phase.ESCAPE && keyComplete && s.alive && !s.hidden && !s.busy &&
            exitState != ExitState.UNLOCKING &&
            s.dist(map.exitApproach.x, map.exitApproach.y) <= GameConfig.EXIT_RANGE

    fun useExit(s: Survivor) {
        if (!canUseExit(s)) return
        s.dir = if (map.exitOrientation == ExitOrientation.EAST) Dir.RIGHT else Dir.UP
        if (exitState == ExitState.LOCKED) {
            s.startAction(SurvivorAction.EXIT_INTERACT, GameConfig.EXIT_INTERACT_TIME)
            sound.playAt("door_unlock", map.exitCenter.x, map.exitCenter.y, 1f)
        } else {
            s.startAction(SurvivorAction.ESCAPING, GameConfig.ESCAPE_ANIM_TIME)
        }
    }

    private fun onActionDone(s: Survivor, a: SurvivorAction) {
        when (a) {
            SurvivorAction.EXIT_INTERACT -> if (exitState == ExitState.LOCKED) {
                exitState = ExitState.UNLOCKING; exitAnimTime = 0f
            }
            SurvivorAction.ESCAPING -> {
                s.status = SurvivorStatus.ESCAPED
                s.hidingIn = null
                if (s === player) showToast("Kamu berhasil kabur!")
            }
            SurvivorAction.KEY_PICKUP -> {}
        }
    }

    // ---------- Aksi pemain ----------
    val playerContext: ContextAction
        get() = when {
            !player.alive || player.busy -> ContextAction.NONE
            player.hidden -> ContextAction.LEAVE
            canUseExit(player) -> ContextAction.EXIT
            nearestUsableSpot(player) != null -> ContextAction.HIDE
            else -> ContextAction.NONE
        }

    private fun handlePlayerAction() {
        when (playerContext) {
            ContextAction.LEAVE -> leaveSpot(player)
            ContextAction.EXIT -> useExit(player)
            ContextAction.HIDE -> nearestUsableSpot(player)?.let { enterSpot(player, it) }
            ContextAction.NONE -> {}
        }
    }

    // ---------- Spectator ----------
    private fun updateSpectator() {
        if (!spectating) return
        val t = spectateTarget
        if (t == null || !t.alive) {
            val alive = bots.filter { it.alive }
            spectateTarget = if (alive.isEmpty()) null
            else alive.minByOrNull { Math.hypot((it.x - (t?.x ?: player.x)).toDouble(), (it.y - (t?.y ?: player.y)).toDouble()) }
        }
    }

    fun cycleSpectate(step: Int) {
        val alive = bots.filter { it.alive }
        if (alive.isEmpty()) return
        val i = alive.indexOf(spectateTarget)
        spectateTarget = alive[((if (i < 0) 0 else i) + step + alive.size) % alive.size]
    }

    // ---------- Persembunyian ----------
    fun nearestUsableSpot(s: Survivor): HidingSpot? {
        var best: HidingSpot? = null
        var bd = GameConfig.HIDE_RANGE
        for (sp in spots) {
            if (!sp.usable) continue
            val d = s.dist(sp.x, sp.y)
            if (d < bd) { bd = d; best = sp }
        }
        return best
    }

    fun enterSpot(s: Survivor, spot: HidingSpot): Boolean {
        if (!spot.usable || !s.alive || s.busy) return false
        if (jason.state != JasonState.COUNTDOWN_LOCKED && vision.canSee(jason, s)) {
            spot.suspicion = Math.max(spot.suspicion, 0.85f)   // Jason melihat orang masuk
        }
        spot.occupant = s
        s.hidingIn = spot
        s.hideAnim = 0f
        s.clearPath()
        s.x = spot.x; s.y = spot.y
        s.sprinting = false
        sound.playAt("hide", spot.x, spot.y, 0.6f)
        return true
    }

    fun leaveSpot(s: Survivor) {
        val spot = s.hidingIn ?: return
        spot.occupant = null
        s.hidingIn = null
        val ny = spot.y - 12f
        if (!map.blocked(spot.x, ny)) s.y = ny
        sound.playAt("hide", spot.x, spot.y, 0.4f)
    }

    // ---------- Konsekuensi visual ----------
    private fun eliminate(s: Survivor) {
        s.status = SurvivorStatus.ELIMINATED
        s.clearAction()
        s.moving = false
        if (s === player) {
            spectateTarget = null
            showToast("Kamu tereliminasi - mode penonton")
        }
    }

    /** Terbunuh di ruang terbuka: mayat + genangan darah persisten. */
    fun killInOpen(s: Survivor) {
        if (!s.alive) return
        s.hidingIn?.occupant = null
        s.hidingIn = null
        eliminate(s)
        gore.openKill(s.prefix, s.x, s.y)
        sound.playAt("gore", s.x, s.y); sound.playAt("scream", s.x, s.y, 0.9f)
        sound.emitNoise(s.x, s.y, GameConfig.NOISE_RADIUS_KILL)
    }

    /** Jason menyerang objek: kosong -> rusak tanpa darah; terisi -> hancur berdarah. */
    fun attackSpot(spot: HidingSpot) {
        val victim = spot.occupant
        if (victim == null) {
            spot.state = SpotState.BROKEN_CLEAN
            gore.breakClean(spot.type.material, spot.x, spot.y)
            sound.playAt("break", spot.x, spot.y)
        } else {
            spot.state = SpotState.BROKEN_BLOODY
            spot.occupant = null
            victim.hidingIn = null
            eliminate(victim)
            gore.breakBloody(spot.type.material, spot.x, spot.y)
            sound.playAt("break", spot.x, spot.y); sound.playAt("gore", spot.x, spot.y)
            sound.playAt("scream", spot.x, spot.y, 0.9f)
        }
        sound.emitNoise(spot.x, spot.y, GameConfig.NOISE_RADIUS_BREAK)
    }

    /** Jason selesai mengecek: jika ada orang, diseret keluar dan dibunuh. */
    fun resolveCheck(spot: HidingSpot) {
        val victim = spot.occupant
        spot.state = SpotState.INTACT
        if (victim != null) {
            spot.occupant = null
            victim.hidingIn = null
            victim.x = spot.x; victim.y = spot.y - 12f
            killInOpen(victim)
        }
    }

    /** Jason kehilangan target: spot di sekitar titik terakhir terlihat jadi lebih mencurigakan. */
    fun onLostSight(x: Float, y: Float) {
        for (sp in spots) {
            val d = Math.hypot((sp.x - x).toDouble(), (sp.y - y).toDouble())
            if (d < 80.0) sp.suspicion = Math.min(1f, sp.suspicion + 0.5f)
        }
    }

    fun dispose() { sound.noiseListeners.clear() }
}
