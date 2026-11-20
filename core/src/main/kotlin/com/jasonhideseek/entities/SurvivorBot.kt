package com.jasonhideseek.entities

import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.systems.GameWorld
import com.jasonhideseek.world.ExitState
import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.KeyFrame
import java.util.Random

/**
 * Bot penyintas offline. Perilaku: Explore, Seek Hiding Spot, Hide, Flee, Search/Collect Key Frame,
 * Seek Exit, Escape. Kondisi dievaluasi ulang secara berkala (decide) sehingga rencana bisa berubah.
 */
class SurvivorBot(prefix: String, x: Float, y: Float, private val rng: Random) : Survivor(prefix, x, y) {
    private enum class Goal { IDLE, HIDE, EXPLORE, KEY, EXIT, FLEE, MERGE }

    private var goal = Goal.IDLE
    private var goalSpot: HidingSpot? = null
    private var goalKey: KeyFrame? = null
    private var decideTimer = rng.nextFloat() * 0.6f
    private var hiddenFor = 0f
    private var fleeCooldown = 0f
    private var exploreIdx = -1
    private val visited = HashSet<Int>()

    val nerve = 0.3f + rng.nextFloat() * 0.6f       // makin tinggi makin mudah panik
    private val greed = 0.35f + rng.nextFloat() * 0.55f  // dorongan mencari Key Frame
    private val minHide = 5f + rng.nextFloat() * 9f
    private var pendingNoise = 0f

    /** Dipanggil SoundCueSystem saat ada suara kerusakan/pembunuhan. */
    fun hear(nx: Float, ny: Float, radius: Float) {
        if (!alive || dist(nx, ny) > radius) return
        pendingNoise = 1f
    }

    fun update(dt: Float, w: GameWorld) {
        animTime += dt
        fleeCooldown -= dt
        if (busy) { moving = false; sprinting = false; return }
        if (hidden) { hideAnim += dt; hiddenFor += dt; moving = false; sprinting = false }

        decideTimer -= dt
        if (decideTimer <= 0f) {
            decide(w)
            decideTimer = 0.35f + rng.nextFloat() * 0.25f
        }
        execute(dt, w)
        updateStamina(dt)
        pendingNoise = 0f
    }

    // ---------- Evaluasi kondisi ----------
    private fun decide(w: GameWorld) {
        val j = w.jason
        val jd = j.dist(x, y)
        val jasonActive = j.state != JasonState.COUNTDOWN_LOCKED

        if (hidden) {
            val spot = hidingIn ?: return
            val jdSpot = j.dist(spot.x, spot.y)
            var panic = 0f
            if (pendingNoise > 0f) panic += 0.5f * nerve
            if (jasonActive && jdSpot < 55f && j.targetSpot !== spot) panic += 0.18f * nerve
            if (jasonActive && j.targetSpot === spot && jdSpot < 70f) panic += 0.35f * nerve
            // Tempat sembunyi mulai retak: makin rusak makin besar dorongan untuk kabur.
            if (spot.damaged && jdSpot < 70f) panic += (0.25f + 0.2f * (GameConfig.SPOT_HP - spot.hp)) * (0.5f + nerve)
            if (fleeCooldown <= 0f && jasonActive && panic > 0f && rng.nextFloat() < panic) {
                fleeCooldown = 6f
                beginFlee(w)
                return
            }
            // Harus bergerak (kunci utuh / pintu terbuka / tim merakit kunci) dan Jason tidak dekat: keluar
            if (w.escapePhase && mustMove(w) && jd > 150f && j.targetSpot !== spot) { chooseObjective(w, true); return }
            // Keluar untuk lanjut mencari Key Frame bila Jason jauh
            if (w.escapePhase && wantsKeys(w) && hiddenFor > minHide && jd > 190f && j.targetSpot !== spot && rng.nextFloat() < greed) {
                chooseObjective(w, true)
            }
            return
        }

        val seen = jasonActive && (jd < 110f || (j.chasing && jd < 170f)) && w.map.lineClear(j.x, j.y, x, y)
        if (seen) {
            // Dekat pintu keluar & berhak keluar: terus lari ke pintu.
            val exitD = dist(w.map.exitApproach.x, w.map.exitApproach.y)
            if (w.escapePhase && (hasCompleteKey || w.exitState != ExitState.LOCKED) && exitD < 110f && jd > 30f) {
                if (goal != Goal.EXIT || !hasPath) { if (setPathTo(w.map, w.map.exitApproach.x, w.map.exitApproach.y)) goal = Goal.EXIT }
                return
            }
            if (goal != Goal.FLEE || !hasPath) {
                val spot = bestSpot(w, true)
                // Sudah dalam posisi aman (spot sangat dekat & jauh dari Jason) -> sembunyi; selain itu kabur.
                val safeSpot = spot != null && dist(spot.x, spot.y) < 28f && j.dist(spot.x, spot.y) > 45f
                val hot = j.chasing && jd < 110f || jd < 60f
                if (hot && !safeSpot) beginFlee(w)
                else if (spot != null && pathToSpot(w, spot)) { goal = Goal.HIDE; goalSpot = spot } else beginFlee(w)
            }
            return
        }
        if (goal == Goal.FLEE && hasPath) return
        chooseObjective(w, false)
    }

    private fun chooseObjective(w: GameWorld, leaveFirst: Boolean) {
        if (w.escapePhase) {
            // Pintu sudah terbuka (ada yang lolos): semua menyusul tanpa perlu kunci.
            if (w.exitState != ExitState.LOCKED || hasCompleteKey) {
                if (leaveFirst && hidden) w.leaveSpot(this)
                if (goal != Goal.EXIT || !hasPath) {
                    if (setPathTo(w.map, w.map.exitApproach.x, w.map.exitApproach.y)) goal = Goal.EXIT
                }
                return
            }
            // Semua frame sudah dipegang beberapa survivor: berkumpul untuk merakit kunci, lalu ke pintu.
            if (teamReady(w)) {
                if (leaveFirst && hidden) w.leaveSpot(this)
                val main = w.teamMain()
                if (main === this || main == null) {
                    if (goal != Goal.EXIT || !hasPath) { if (setPathTo(w.map, w.map.exitApproach.x, w.map.exitApproach.y)) goal = Goal.EXIT }
                } else if (setPathTo(w.map, main.x, main.y)) goal = Goal.MERGE
                return
            }
            if (!wantsKeys(w)) {
                // Orang lain yang memegang Key Frame: tunggu dia tereliminasi, sembunyi dulu.
                if (!hidden && goal != Goal.HIDE) {
                    val spot = bestSpot(w, false)
                    if (spot != null && pathToSpot(w, spot)) { goal = Goal.HIDE; goalSpot = spot }
                }
                return
            }
            // Key Frame yang terlihat -> ambil; kalau tidak, jelajahi titik kandidat
            val k = visibleKey(w)
            if (k != null) {
                if (leaveFirst && hidden) w.leaveSpot(this)
                if (goalKey !== k || !hasPath) {
                    if (setPathTo(w.map, k.x, k.y)) { goal = Goal.KEY; goalKey = k }
                }
                return
            }
            if (hidden && !leaveFirst) return
            if (goal == Goal.EXPLORE && hasPath) return
            if (leaveFirst && hidden) w.leaveSpot(this)
            nextExplore(w)
            return
        }
        // Countdown: sebagian besar bot mencari tempat sembunyi
        if (!hidden && goal != Goal.HIDE) {
            val spot = bestSpot(w, false)
            if (spot != null && rng.nextFloat() < 0.75f && pathToSpot(w, spot)) { goal = Goal.HIDE; goalSpot = spot }
            else if (goal != Goal.EXPLORE || !hasPath) nextExplore(w)
        }
    }

    private fun teamReady(w: GameWorld) = heldKeys.isNotEmpty() && w.allFramesHeld && !w.keyComplete
    private fun mustMove(w: GameWorld) = hasCompleteKey || teamReady(w) || w.exitState != ExitState.LOCKED

    /** Bot hanya mengejar Key Frame bila punya kunci/frame, atau belum ada yang memegang frame. */
    private fun wantsKeys(w: GameWorld) = hasCompleteKey || heldKeys.isNotEmpty() || w.keyHolder == null

    private fun visibleKey(w: GameWorld): KeyFrame? {
        var best: KeyFrame? = null
        var bd = GameConfig.KEY_BOT_SIGHT
        for (k in w.keys) {
            if (!k.available) continue
            val d = dist(k.x, k.y)
            if (d < bd && w.map.lineClear(x, y, k.x, k.y)) { bd = d; best = k }
        }
        return best
    }

    private fun nextExplore(w: GameWorld) {
        val cands = w.map.keyCandidates
        if (cands.isEmpty()) return
        if (visited.size >= cands.size) visited.clear()
        var best = -1
        var bs = Float.MAX_VALUE
        for ((i, c) in cands.withIndex()) {
            if (i in visited) continue
            val s = dist(c.x, c.y) + rng.nextFloat() * 120f
            if (s < bs) { bs = s; best = i }
        }
        if (best < 0) return
        exploreIdx = best
        val c = cands[best]
        if (setPathTo(w.map, c.x, c.y)) goal = Goal.EXPLORE else visited.add(best)
    }

    private fun bestSpot(w: GameWorld, avoidJason: Boolean): HidingSpot? {
        var best: HidingSpot? = null
        var bs = Float.MAX_VALUE
        for (s in w.spots) {
            if (!s.usable) continue
            val dj = w.jason.dist(s.x, s.y)
            if (avoidJason && dj < 45f) continue
            var score = dist(s.x, s.y) + rng.nextFloat() * 70f
            if (avoidJason) score -= dj * 0.4f
            if (score < bs) { bs = score; best = s }
        }
        return best
    }

    private fun pathToSpot(w: GameWorld, s: HidingSpot): Boolean = setPathTo(w.map, s.x, s.y)

    private fun beginFlee(w: GameWorld) {
        if (hidden) w.leaveSpot(this)
        val j = w.jason
        var best: com.badlogic.gdx.math.Vector2? = null
        var bs = -Float.MAX_VALUE
        val tiles = w.map.floorTiles
        repeat(8) {
            val t = tiles[rng.nextInt(tiles.size)]
            val s = j.dist(t.x, t.y) - 0.35f * dist(t.x, t.y)
            if (s > bs) { bs = s; best = t }
        }
        val b = best
        if (b != null && setPathTo(w.map, b.x, b.y)) goal = Goal.FLEE else goal = Goal.IDLE
    }

    // ---------- Eksekusi ----------
    private fun execute(dt: Float, w: GameWorld) {
        if (hidden) return
        val j = w.jason
        val danger = j.state != JasonState.COUNTDOWN_LOCKED && j.dist(x, y) < 130f
        val wantSprint = goal == Goal.FLEE || (danger && (goal == Goal.HIDE || goal == Goal.EXIT))
        sprinting = wantSprint && !exhausted && stamina > 0f
        val speed = if (sprinting) GameConfig.BOT_SPRINT else GameConfig.BOT_SPEED

        when (goal) {
            Goal.IDLE -> { moving = false }
            Goal.HIDE -> {
                val s = goalSpot
                if (s == null || !s.usable) { goal = Goal.IDLE; goalSpot = null; decideTimer = 0f; return }
                val arrived = followPath(dt, speed, w.map)
                if (arrived || dist(s.x, s.y) < 14f) {
                    if (w.enterSpot(this, s)) { hiddenFor = 0f }
                    goal = Goal.IDLE; goalSpot = null
                }
            }
            Goal.EXPLORE -> {
                if (followPath(dt, speed, w.map)) {
                    if (exploreIdx >= 0) visited.add(exploreIdx)
                    goal = Goal.IDLE; decideTimer = 0f
                }
            }
            Goal.KEY -> {
                val k = goalKey
                if (k == null || !k.available) { goal = Goal.IDLE; goalKey = null; decideTimer = 0f; return }
                if (followPath(dt, speed, w.map)) { goal = Goal.IDLE; decideTimer = 0f }
            }
            Goal.EXIT -> {
                if (w.canUseExit(this)) { w.useExit(this); return }
                val near = dist(w.map.exitApproach.x, w.map.exitApproach.y) <= GameConfig.EXIT_RANGE
                if (near) { moving = false; if (w.exitState == ExitState.UNLOCKING) return }
                else if (followPath(dt, speed, w.map)) { moving = false }
            }
            Goal.FLEE -> {
                if (followPath(dt, speed, w.map)) { goal = Goal.IDLE; decideTimer = 0f }
            }
            Goal.MERGE -> {
                val m = w.teamMain()
                if (m == null || m === this || w.keyComplete) { goal = Goal.IDLE; decideTimer = 0f; return }
                // Menempel ke pembawa utama sampai kunci dirakit
                if (dist(m.x, m.y) <= GameConfig.ASSEMBLE_RANGE * 0.6f) { moving = false; return }
                if (followPath(dt, speed, w.map)) { moving = false }
            }
        }
    }
}
