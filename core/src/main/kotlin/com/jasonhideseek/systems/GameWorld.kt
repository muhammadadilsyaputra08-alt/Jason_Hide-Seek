package com.jasonhideseek.systems

import com.jasonhideseek.ai.JasonDecisionEngine
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.entities.JasonEntity
import com.jasonhideseek.entities.JasonState
import com.jasonhideseek.entities.Player
import com.jasonhideseek.entities.Survivor
import com.jasonhideseek.entities.SurvivorBot
import com.jasonhideseek.game.JasonGame
import com.jasonhideseek.ui.TouchControls
import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.MapDefinition
import com.jasonhideseek.world.SpotState
import java.util.Random

enum class Phase { COUNTDOWN, HUNT, ENDED }
enum class RoundResult { WIN, LOSE }

/** Simulasi satu ronde: fase, entitas, keputusan menang/kalah, dan konsekuensi visual. */
class GameWorld(game: JasonGame, mapIndex: Int) {
    val map = MapDefinition(mapIndex)
    val rng = Random()
    val sound = game.sound
    val gore = GoreAndDebrisSystem(game.assets, rng)
    val vision = VisionSystem(map)
    val spots = ArrayList<HidingSpot>()
    val bots = ArrayList<SurvivorBot>()
    val survivors = ArrayList<Survivor>()
    val player = Player(map.playerSpawn.x, map.playerSpawn.y)
    val jason = JasonEntity(map.jasonSpawn.x, map.jasonSpawn.y, JasonDecisionEngine(rng), rng)

    var phase = Phase.COUNTDOWN
        private set
    var phaseRemaining = GameConfig.COUNTDOWN_TIME
        private set
    var result: RoundResult? = null
        private set
    var endTimer = 0f
        private set
    var tension = 0f
        private set

    val huntStarted get() = phase == Phase.HUNT
    val huntProgress get() = if (phase == Phase.HUNT) 1f - phaseRemaining / GameConfig.HUNT_TIME else 0f
    val survivorsAlive get() = survivors.count { it.alive }
    val nearbySpot: HidingSpot? get() = nearestUsableSpot(player)

    init {
        for ((t, x, y) in map.spotDefs) spots.add(HidingSpot(t, x, y))
        survivors.add(player)
        val names = listOf("bot1", "bot2", "bot3", "bot4")
        val pool = map.floorTiles.filter {
            Math.hypot((it.x - map.jasonSpawn.x).toDouble(), (it.y - map.jasonSpawn.y).toDouble()) > 220.0 &&
                Math.hypot((it.x - map.playerSpawn.x).toDouble(), (it.y - map.playerSpawn.y).toDouble()) > 24.0
        }
        for (i in 0 until GameConfig.BOT_COUNT) {
            val t = pool[rng.nextInt(pool.size)]
            val b = SurvivorBot(names[i % names.size], t.x, t.y, rng)
            bots.add(b); survivors.add(b)
        }
        sound.noiseListeners.clear()
        for (b in bots) sound.noiseListeners.add { nx, ny, r -> b.hear(nx, ny, r) }
    }

    fun update(dt: Float, controls: TouchControls) {
        gore.update(dt)
        for (s in spots) {
            if (s.recheckCooldown > 0f) s.recheckCooldown -= dt
            if (s.suspicion > 0f) s.suspicion = Math.max(0f, s.suspicion - GameConfig.SUSPICION_DECAY * dt)
        }
        if (phase == Phase.ENDED) { endTimer += dt; jason.update(dt, this); return }

        // Timer fase
        phaseRemaining -= dt
        if (phase == Phase.COUNTDOWN && phaseRemaining <= 0f) {
            phase = Phase.HUNT
            phaseRemaining = GameConfig.HUNT_TIME
            jason.release()
            sound.play("alert", 0.9f)
        }

        // Input hide / exit pemain
        if (controls.consumeAction()) {
            if (player.alive) {
                if (player.hidden) leaveSpot(player)
                else nearestUsableSpot(player)?.let { enterSpot(player, it) }
            }
        }
        player.update(dt, controls, map)
        for (b in bots) if (b.alive) b.update(dt, this)
        jason.update(dt, this)

        sound.listenerX = player.x; sound.listenerY = player.y
        updateTension()
        sound.updateHeartbeat(dt, tension)
        evaluateRound()
    }

    private fun updateTension() {
        tension = when {
            phase != Phase.HUNT || !player.alive -> 0f
            jason.state == JasonState.CHASE -> 1f
            else -> {
                val d = jason.dist(player.x, player.y)
                val base = (1f - d / 220f).coerceIn(0f, 1f) * 0.75f
                if (player.hidden && jason.targetSpot === player.hidingIn) Math.max(base, 0.9f) else base
            }
        }
    }

    private fun evaluateRound() {
        if (!player.alive) { finish(RoundResult.LOSE); return }
        if (phase == Phase.HUNT) {
            if (phaseRemaining <= 0f) { finish(RoundResult.WIN); return }
            if (bots.none { it.alive }) finish(RoundResult.WIN)
        }
    }

    private fun finish(r: RoundResult) {
        result = r; phase = Phase.ENDED; endTimer = 0f
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
        if (!spot.usable || !s.alive) return false
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
    /** Terbunuh di ruang terbuka: mayat + genangan darah persisten. */
    fun killInOpen(s: Survivor) {
        if (!s.alive) return
        s.alive = false
        s.hidingIn?.occupant = null
        s.hidingIn = null
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
            victim.alive = false
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
