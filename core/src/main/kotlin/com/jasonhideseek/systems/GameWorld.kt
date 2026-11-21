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
import com.jasonhideseek.world.ArrivalSequence
import com.jasonhideseek.world.ArrivalStage
import com.jasonhideseek.world.ExitOrientation
import com.jasonhideseek.world.ExitState
import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.KeyFrame
import com.jasonhideseek.world.MapRepository
import com.jasonhideseek.world.SpotState
import java.util.Random

enum class Phase { COUNTDOWN, ESCAPE, ENDED }

enum class FeedKind { KILL, KEY, ESCAPE, INFO, BUFF }

/** Satu baris notifikasi (kill feed) di HUD. */
class FeedItem(val text: String, val kind: FeedKind, var ttl: Float)

/** Tombol aksi kontekstual pemain. */
enum class ContextAction { NONE, HIDE, LEAVE, EXIT, ATTACK }

/** Simulasi satu pertandingan: fase, survivor, Key Frame, pintu keluar, spectator, dan hasil akhir. */
class GameWorld(game: JasonGame, private val mapIndex: Int, val jasonMode: Boolean = false) {
    val map = MapRepository.load(mapIndex).also { it.resetChamber() }
    val rng = Random()
    val sound = game.sound
    val gore = GoreAndDebrisSystem(game.assets, rng)
    val vision = VisionSystem(map)
    val spots = ArrayList<HidingSpot>()
    val bots = ArrayList<SurvivorBot>()
    val survivors = ArrayList<Survivor>()
    val keys = ArrayList<KeyFrame>()
    private val spawnPlan = SpawnPlanner.plan(map, rng, GameConfig.BOT_COUNT + 1)
    val player = Player(spawnPlan.survivors[0].x, spawnPlan.survivors[0].y)
    val jason = JasonEntity(spawnPlan.jason.x, spawnPlan.jason.y, JasonDecisionEngine(rng), rng)
    val buffs = JasonBuffs(rng)
    /** Linimasa pintu Ruang Kedatangan Jason (null bila map tanpa ruang). */
    val arrival: ArrivalSequence? = map.arrival?.let { ArrivalSequence(it) }
    private var lastArrivalStage = ArrivalStage.LOCKED
    /** Mini camera Jason aktif hanya setelah Jason mulai bergerak (keluar dari ruang). */
    val miniCamOnline get() = jason.state != JasonState.COUNTDOWN_LOCKED
    val effect = MapEffect.forMap(mapIndex)
    val feed = ArrayList<FeedItem>()
    /** Status efek map. */
    var blackout = false
        private set
    var blackoutAlpha = 0f
        private set
    var alarmLeft = 0f
        private set
    var alarmX = 0f
        private set
    var alarmY = 0f
        private set
    private var fxTimer = if (effect == MapEffect.CAR_ALARM) 22f else 30f
    private var creakTimer = 0f
    private var alarmPulse = 0f
    /** Pengali jarak pandang Key Frame (kabut / gelap). */
    val keyViewMul get() = (if (effect == MapEffect.FOG) 0.6f else 1f) * (if (blackout) 0.6f else 1f)
    var bannerText: String? = null
        private set
    var bannerKind = FeedKind.INFO
        private set
    var bannerTimer = 0f
        private set
    private val bannerQueue = java.util.ArrayDeque<Pair<String, FeedKind>>()
    var escaperName: String? = null
        private set

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

    var exitState = ExitState.LOCKED
        private set
    var exitAnimTime = 0f
        private set

    /** Bot yang sedang diamati saat pemain tereliminasi. */
    var spectateTarget: Survivor? = null
        private set

    /** Key Frame milik pemain. */
    val keysCollected get() = player.heldKeys.size
    /** Ada survivor hidup yang memegang kunci utuh (3 frame). */
    val keyComplete get() = survivors.any { it.alive && it.hasCompleteKey }
    /** Survivor hidup yang memegang Key Frame terbanyak (null bila tak ada). */
    val keyHolder: Survivor? get() = survivors.filter { it.alive && it.heldKeys.isNotEmpty() }.maxByOrNull { it.heldKeys.size }
    /** Survivor hidup yang memegang kunci utuh (target buruan Jason). */
    val completeKeyHolder: Survivor? get() = survivors.firstOrNull { it.alive && it.hasCompleteKey }
    val keysOnGround get() = keys.count { !it.collected }
    fun label(s: Survivor) = if (s === player && !jasonMode) "${s.name} (Kamu)" else s.name
    val escapePhase get() = phase == Phase.ESCAPE
    val escapeProgress get() = if (phase == Phase.ESCAPE) 1f - phaseRemaining / GameConfig.ESCAPE_TIME else 0f
    val huntProgress get() = escapeProgress
    val survivorsAlive get() = survivors.count { it.alive }
    val escapedCount get() = survivors.count { it.status == SurvivorStatus.ESCAPED }
    val eliminatedCount get() = survivors.count { it.status == SurvivorStatus.ELIMINATED }
    val spectating get() = !jasonMode && !player.alive && (player.status == SurvivorStatus.ELIMINATED || player.status == SurvivorStatus.ESCAPED)

    /** Survivor yang menjadi fokus kamera utama. */
    val focus: com.jasonhideseek.entities.Character get() = if (jasonMode) jason else if (spectating) (spectateTarget ?: player) else player

    init {
        for (d in map.spotDefs) spots.add(HidingSpot(d.type, d.x, d.y))
        if (jasonMode) { player.status = SurvivorStatus.ELIMINATED; jason.playerControlled = true } else survivors.add(player)
        val botCount = if (jasonMode) GameConfig.BOT_COUNT + 1 else GameConfig.BOT_COUNT
        val off = if (jasonMode) 0 else 1

        // Nama survivor (acak) + spawn acak untuk semua survivor
        val names = NAMES.toMutableList(); names.shuffle(rng)
        player.name = names[0]
        for (i in 0 until botCount) {
            val t = spawnPlan.survivors[i + off]
            val b = SurvivorBot(if (i + 1 > GameConfig.BOT_COUNT) "player" else "bot${i + 1}", t.x, t.y, rng)
            b.name = names[(i + off) % names.size]
            bots.add(b); survivors.add(b)
        }

        // 3 Key Frame: tersebar jauh satu sama lain dan jauh dari titik spawn
        val cands = map.keyCandidates.filter { !map.isSolidTile(it.col, it.row) }.toMutableList()
        cands.shuffle(rng)
        val spawnPts = spawnPlan.survivors + spawnPlan.jason
        val scratch = ArrayList<com.badlogic.gdx.math.Vector2>()
        var spacing = GameConfig.KEY_MIN_SPACING
        var fromSpawn = GameConfig.KEY_MIN_FROM_SPAWN
        var tries = 0
        while (keys.size < GameConfig.KEY_FRAME_COUNT && tries < 6) {
            for (c in cands) {
                if (keys.size >= GameConfig.KEY_FRAME_COUNT) break
                if (keys.any { Math.hypot((it.x - c.x).toDouble(), (it.y - c.y).toDouble()) < spacing }) continue
                if (spawnPts.any { Math.hypot((it.x - c.x).toDouble(), (it.y - c.y).toDouble()) < fromSpawn }) continue
                if (!map.findPath(map.exitApproach.x, map.exitApproach.y, c.x, c.y, scratch)) continue
                keys.add(KeyFrame(keys.size + 1, c.x, c.y))
            }
            spacing *= 0.75f; fromSpawn *= 0.75f; tries++
        }

        sound.noiseListeners.clear()
        sound.noiseMul = if (effect == MapEffect.ECHO) 1.6f else 1f
        for (b in bots) sound.noiseListeners.add { nx, ny, r -> b.hear(nx, ny, r) }
        sound.noiseListeners.add { nx, ny, r -> jason.hear(nx, ny, r, buffs.hearChance, this) }
        pushFeed("Efek: ${effect.title}", FeedKind.INFO)
    }

    // ---------- Loop ----------
    fun update(dt: Float, controls: TouchControls) {
        gore.update(dt)
        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) {
                val nx = bannerQueue.poll()
                if (nx != null) { bannerText = nx.first; bannerKind = nx.second; bannerTimer = BANNER_TIME } else bannerText = null
            }
        }
        val fi = feed.iterator()
        while (fi.hasNext()) { val f = fi.next(); f.ttl -= dt; if (f.ttl <= 0f) fi.remove() }
        for (s in spots) {
            if (s.recheckCooldown > 0f) s.recheckCooldown -= dt
            if (s.suspicion > 0f) s.suspicion = Math.max(0f, s.suspicion - GameConfig.SUSPICION_DECAY * dt)
        }
        if (exitState == ExitState.UNLOCKING) {
            exitAnimTime += dt
            if (exitAnimTime >= GameConfig.EXIT_UNLOCK_TIME) {
                exitState = ExitState.OPEN
                showBanner("Pintu keluar terbuka - yang lain bisa menyusul!", FeedKind.ESCAPE)
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
        if (phase == Phase.COUNTDOWN) updateArrival(dt)
        if (phase == Phase.COUNTDOWN && phaseRemaining <= 0f) {
            phase = Phase.ESCAPE
            phaseRemaining = GameConfig.ESCAPE_TIME
            if (arrival != null && arrival.stage != ArrivalStage.BROKEN) { arrival.update(1000f); onArrivalStage(ArrivalStage.BROKEN) }
            jason.release()
            if (arrival != null) showBanner("Jason keluar dari ruangannya!", FeedKind.KILL)
            sound.play("alert", 0.9f)
            showBanner(if (jasonMode) "Jason dilepas - buru semua survivor!" else "Cari 3 Key Frame - hanya pemegang kunci utuh yang bisa keluar!", FeedKind.INFO)
        }

        val wantsAction = controls.consumeAction()
        for (sv in survivors) if (sv.pickupCooldown > 0f) sv.pickupCooldown -= dt
        for (k in keys) if (k.claimer == null && k.progress > 0f) k.progress = Math.max(0f, k.progress - 0.05f * dt)
        buffs.escapes = escapedCount
        // Pemain bisa membatalkan pengambilan frame kapan saja (gerak / tombol aksi)
        if (!jasonMode && player.action == SurvivorAction.KEY_PICKUP &&
            (Math.hypot(controls.moveX.toDouble(), controls.moveY.toDouble()) > 0.3 || wantsAction)) cancelPickup(player)
        if (jasonMode) {
            if (wantsAction && phase == Phase.ESCAPE) jason.playerAttack(this)
        } else {
            if (wantsAction && player.alive && !player.busy) handlePlayerAction()
            player.update(dt, controls, map)
        }
        for (b in bots) if (b.alive) b.update(dt, this)
        for (s in survivors) {
            if (!s.alive) continue
            val done = s.tickAction(dt)
            if (done != null) onActionDone(s, done)
        }
        buffs.update(dt)
        updateEffects(dt)
        jason.update(dt, this, controls)
        // Bot membatalkan pengambilan frame bila Jason mendekat
        if (jason.state != JasonState.COUNTDOWN_LOCKED) {
            for (b in bots) {
                if (!b.alive || b.action != SurvivorAction.KEY_PICKUP) continue
                if (jason.dist(b.x, b.y) < GameConfig.PICKUP_ABORT_JASON && (jason.chasing || vision.canSee(jason, b))) cancelPickup(b)
            }
        }
        checkAssembly()

        if (phase == Phase.ESCAPE) checkKeyPickups()
        updateSpectator()

        val f = focus
        sound.listenerX = f.x; sound.listenerY = f.y
        updateTension()
        sound.updateHeartbeat(dt, tension)
        evaluateRound()
    }

    /** Linimasa pintu dimulai agar selesai tepat saat countdown berakhir. */
    private fun updateArrival(dt: Float) {
        val a = arrival ?: return
        if (GameConfig.COUNTDOWN_TIME - phaseRemaining < GameConfig.COUNTDOWN_TIME - a.def.totalDuration) return
        a.update(dt)
        val st = a.stage
        if (st != lastArrivalStage) onArrivalStage(st)
    }

    private fun onArrivalStage(st: ArrivalStage) {
        lastArrivalStage = st
        val d = map.arrival ?: return
        val x = map.cellCenter(d.doorCol, d.doorRow).x; val y = map.cellCenter(d.doorCol, d.doorRow).y
        when (st) {
            ArrivalStage.WARNING -> { sound.playAt("door_locked", x, y, 1f); sound.playAt("creak", x, y, 1f); showBanner("Ada sesuatu menggedor dari balik pintu...", FeedKind.INFO) }
            ArrivalStage.BREAKING -> { jason.breakingDoor = true; sound.playAt("break", x, y, 1f); sound.emitNoise(x, y, 260f) }
            ArrivalStage.BROKEN -> { map.openChamberDoor(); sound.playAt("break", x, y, 1f); sound.emitNoise(x, y, 320f) }
            ArrivalStage.LOCKED -> {}
        }
    }

    private fun updateEffects(dt: Float) {
        vision.viewMul = buffs.viewMul * (if (effect == MapEffect.FOG) 0.7f else 1f) * (if (blackout) 0.5f else 1f)
        vision.proxMul = buffs.proxMul
        vision.fovExtra = buffs.fovExtra
        blackoutAlpha += ((if (blackout) 0.7f else 0f) - blackoutAlpha) * Math.min(1f, dt * 4f)
        if (phase != Phase.ESCAPE) return
        when (effect) {
            MapEffect.CREAKY_FLOORS -> {
                creakTimer -= dt
                if (creakTimer <= 0f) {
                    creakTimer = 0.55f
                    for (s in survivors) {
                        if (!s.alive || s.hidden || !s.moving || !s.sprinting) continue
                        sound.emitNoise(s.x, s.y, GameConfig.CREAK_NOISE_RADIUS)
                        if (s === player) sound.play("creak", 0.35f)
                    }
                }
            }
            MapEffect.CAR_ALARM -> {
                if (alarmLeft > 0f) {
                    alarmLeft -= dt; alarmPulse -= dt
                    if (alarmPulse <= 0f) { alarmPulse = 0.9f; sound.playAt("alert", alarmX, alarmY, 1f) }
                }
                fxTimer -= dt
                if (fxTimer <= 0f) {
                    fxTimer = 32f + rng.nextFloat() * 14f
                    val t = map.reachableFloorTiles()[rng.nextInt(map.reachableFloorTiles().size)]
                    alarmX = t.x; alarmY = t.y; alarmLeft = 4f; alarmPulse = 0f
                    sound.emitNoise(alarmX, alarmY, GameConfig.ALARM_NOISE_RADIUS)
                    jason.hear(alarmX, alarmY, 99999f, 1f, this)
                    showBanner("Alarm mobil berbunyi!", FeedKind.INFO)
                }
            }
            MapEffect.BLACKOUT -> {
                fxTimer -= dt
                if (fxTimer <= 0f) {
                    blackout = !blackout
                    fxTimer = if (blackout) 7f else 38f
                    if (blackout) showBanner("Listrik padam!", FeedKind.INFO) else pushFeed("Listrik menyala kembali", FeedKind.INFO)
                }
            }
            else -> {}
        }
    }

    val alarmActive get() = alarmLeft > 0f

    private fun grantBuff(b: JasonBuff, reason: String) {
        buffs.grant(b)
        pushFeed("Jason +${b.title}", FeedKind.BUFF)
    }

    private fun updateTension() {
        tension = when {
            jasonMode || phase != Phase.ESCAPE || !player.alive -> 0f
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
        val r = MatchRules.evaluate(if (jasonMode) SurvivorStatus.ALIVE else player.status, botsEscaped, survivorsAlive, timeUp) ?: return
        result = r; phase = Phase.ENDED; endTimer = 0f
        escaperName = survivors.filter { it.status == SurvivorStatus.ESCAPED }.joinToString(", ") { label(it) }.ifEmpty { null }
        jason.setEnded(r == MatchResult.JASON_WINS)
        sound.play(if (r == MatchResult.JASON_WINS || r == MatchResult.TIMES_UP) "lose" else "win", 0.9f)
    }

    fun summary() = MatchSummary(
        result ?: MatchResult.TIMES_UP, map.name, if (jasonMode) SurvivorStatus.ALIVE else player.status,
        escapedCount, eliminatedCount, survivors.size, elapsed.toInt(),
        if (jasonMode) (keyHolder?.heldKeys?.size ?: 0) else keysCollected, escaperName, jasonMode, jasonVictory
    )

    /** Mode Jason: menang bila kurang dari separuh survivor yang berhasil kabur. */
    val jasonVictory get() = escapedCount * 2 < survivors.size

    // ---------- Key Frame ----------
    /** Bot hanya mengambil frame bila dirinya sudah membawa frame, atau belum ada survivor lain yang membawa. */
    private fun mayCollect(s: Survivor): Boolean =
        s === player || s.heldKeys.isNotEmpty() || survivors.none { it !== s && it.alive && it.heldKeys.isNotEmpty() }

    private fun checkKeyPickups() {
        for (k in keys) {
            if (!k.available) continue
            for (s in survivors) {
                if (!s.alive || s.hidden || s.busy || s.hasCompleteKey || s.pickupCooldown > 0f || !mayCollect(s)) continue
                if (jason.chasing && jason.dist(k.x, k.y) < 160f) continue   // jangan mulai saat Jason mengejar di dekat sini
                if (s.dist(k.x, k.y) <= GameConfig.KEY_PICKUP_RANGE) { beginPickup(k, s); break }
            }
        }
    }

    private fun beginPickup(k: KeyFrame, s: Survivor) {
        k.claimer = s
        s.startAction(SurvivorAction.KEY_PICKUP, GameConfig.KEY_PICKUP_TIME * (1f - k.progress))
        sound.playAt("key_pickup", k.x, k.y, 0.7f)
        sound.emitNoise(k.x, k.y, GameConfig.KEY_NOISE_RADIUS)
    }

    /** Batalkan pengambilan: progres disimpan sehingga bisa dilanjutkan. */
    private fun cancelPickup(s: Survivor) {
        val f = s.actionProgress
        for (k in keys) if (k.claimer === s) {
            k.progress = (k.progress + (1f - k.progress) * f).coerceIn(0f, 0.95f)
            k.claimer = null
        }
        s.clearAction()
        s.pickupCooldown = GameConfig.PICKUP_COOLDOWN
    }

    val allFramesHeld get() = keys.isNotEmpty() && keys.all { it.holder != null }

    /** Pembawa frame terbanyak (inti tim perakit kunci). */
    fun teamMain(): Survivor? = survivors.filter { it.alive && it.heldKeys.isNotEmpty() }.maxByOrNull { it.heldKeys.size }

    /** Frame terbagi ke beberapa survivor: bila semuanya berdekatan, frame dirakit jadi satu kunci pada pembawa utama. */
    private fun checkAssembly() {
        if (phase != Phase.ESCAPE || !allFramesHeld || completeKeyHolder != null) return
        val team = survivors.filter { it.alive && it.heldKeys.isNotEmpty() }
        if (team.size < 2) return
        val main = teamMain() ?: return
        if (main.hidden || main.busy) return
        if (team.any { it !== main && (it.hidden || it.busy || it.dist(main.x, main.y) > GameConfig.ASSEMBLE_RANGE) }) return
        for (t in team) if (t !== main) { main.heldKeys.addAll(t.heldKeys); t.heldKeys.clear() }
        for (k in main.heldKeys) k.holder = main
        gore.animFx("fx_key_assembly", main.x, main.y + 22f, 10f)
        sound.play("key_assembly", 1f)
        pushFeed("${short(main)} merakit KUNCI UTUH", FeedKind.KEY)
        showBanner(if (team.any { it === player }) "Kunci dirakit! Menuju pintu keluar" else "${main.name} merakit kunci - Jason memburunya!", FeedKind.KEY)
    }

    private fun finishPickup(s: Survivor) {
        for (k in keys) {
            if (k.claimer !== s) continue
            k.claimer = null; k.holder = s; k.progress = 0f; s.heldKeys.add(k)
            gore.animFx("fx_key_pickup", k.x, k.y + 8f, 14f)
            sound.playAt("key_pickup", k.x, k.y, 1f)
            if (s.hasCompleteKey) {
                gore.animFx("fx_key_assembly", s.x, s.y + 22f, 10f)
                sound.play("key_assembly", 1f)
                pushFeed("${short(s)} pegang KUNCI UTUH", FeedKind.KEY)
                showBanner(if (s === player) "Kunci utuh! Menuju pintu keluar" else "${s.name} pegang kunci utuh - Jason memburunya!", FeedKind.KEY)
            } else {
                pushFeed("${short(s)}: Frame ${s.heldKeys.size}/${GameConfig.KEY_FRAME_COUNT}", FeedKind.KEY)
            }
        }
    }

    /** Jatuhkan semua Key Frame milik survivor di sekitar posisinya (dipanggil saat tereliminasi). */
    private fun dropKeys(s: Survivor) {
        for (k in keys) if (k.claimer === s) k.claimer = null
        if (s.heldKeys.isEmpty()) return
        val offs = arrayOf(0f to 0f, 14f to 0f, -14f to 0f, 0f to 14f, 0f to -14f, 14f to 14f, -14f to 14f, 14f to -14f, -14f to -14f, 28f to 0f, -28f to 0f, 0f to 28f, 0f to -28f)
        var n = 0
        for (k in s.heldKeys) {
            var placed = false
            for (j in offs.indices) {
                val (ox, oy) = offs[(j + n) % offs.size]
                if (!map.blocked(s.x + ox, s.y + oy)) { k.x = s.x + ox; k.y = s.y + oy; placed = true; break }
            }
            if (!placed) {
                val t = map.reachableFloorTiles().minByOrNull { Math.hypot((it.x - s.x).toDouble(), (it.y - s.y).toDouble()) }
                if (t != null) { k.x = t.x; k.y = t.y }
            }
            k.holder = null
            n++
        }
        pushFeed("${short(s)} jatuhkan ${s.heldKeys.size} Frame", FeedKind.KEY)
        s.heldKeys.clear()
    }

    /** Feed kiri: ringkas, tanpa duplikat, maksimal 4 baris. */
    private fun pushFeed(text: String, kind: FeedKind) {
        if (feed.lastOrNull()?.text == text) return
        feed.add(FeedItem(text, kind, 5f))
        while (feed.size > 4) feed.removeAt(0)
    }

    /** Banner tengah: satu per satu (antrean), tidak pernah saling menimpa/menumpuk. */
    private fun showBanner(t: String, kind: FeedKind) {
        if (bannerText == t || bannerQueue.any { it.first == t }) return
        if (bannerText == null) { bannerText = t; bannerKind = kind; bannerTimer = BANNER_TIME }
        else { if (bannerQueue.size >= 2) bannerQueue.poll(); bannerQueue.add(t to kind) }
    }

    fun short(s: Survivor) = if (s === player) "Kamu" else s.name

    // ---------- Pintu keluar ----------
    fun canUseExit(s: Survivor): Boolean =
        phase == Phase.ESCAPE && (s.hasCompleteKey || exitState == ExitState.OPEN) && s.alive && !s.hidden && !s.busy &&
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
                pushFeed("${short(s)} KABUR!", FeedKind.ESCAPE)
                pushFeed("Jason makin agresif!", FeedKind.BUFF)
                showBanner(if (s === player) "Kamu berhasil kabur!" else "${s.name} berhasil kabur!", FeedKind.ESCAPE)
            }
            SurvivorAction.KEY_PICKUP -> finishPickup(s)
        }
    }

    // ---------- Aksi pemain ----------
    val playerContext: ContextAction
        get() = when {
            jasonMode -> if (phase == Phase.ESCAPE && !jason.isSwinging) ContextAction.ATTACK else ContextAction.NONE
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
            ContextAction.NONE, ContextAction.ATTACK -> {}
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
        pushFeed("${short(s)} tereliminasi", FeedKind.KILL)
        dropKeys(s)
        grantBuff(buffs.randomBuff(), "eliminasi")
        showBanner("${label(s)} tereliminasi", FeedKind.KILL)
        if (s === player) spectateTarget = null
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
            buffs.rollBreak()?.let { grantBuff(it, "menghancurkan barang") }
            gore.breakClean(spot.type.material, spot.x, spot.y)
            sound.playAt("break", spot.x, spot.y)
        } else {
            spot.hp -= 1
            sound.emitNoise(spot.x, spot.y, GameConfig.NOISE_RADIUS_BREAK)
            if (spot.hp > 0) {
                // Masih bertahan: retak dulu, penghuni punya waktu kabur.
                sound.playAt("break", spot.x, spot.y, 0.6f)
                sound.playAt("creak", spot.x, spot.y, 0.8f)
                return
            }
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

    companion object {
        const val BANNER_TIME = 2.2f
        val NAMES = listOf("Raka", "Maya", "Bima", "Sari", "Dewi", "Joko", "Tia", "Fajar", "Nina", "Rio")
    }
}
