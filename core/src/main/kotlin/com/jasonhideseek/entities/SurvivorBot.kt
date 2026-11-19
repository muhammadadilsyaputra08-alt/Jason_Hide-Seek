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
    private enum class Goal { IDLE, HIDE, EXPLORE, KEY, EXIT, FLEE }

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
            if (fleeCooldown <= 0f && jasonActive && panic > 0f && rng.nextFloat() < panic) {
                fleeCooldown = 6f
                beginFlee(w)
                return
            }
            // Keluar untuk lanjut mencari Key Frame / pintu bila Jason jauh
            if (w.escapePhase && hiddenFor > minHide && jd > 190f && j.targetSpot !== spot && rng.nextFloat() < greed) {
                chooseObjective(w, true)
            }
            return
        }

        val seen = jasonActive && (jd < 110f || (j.chasing && jd < 170f)) && w.map.lineClear(j.x, j.y, x, y)
        if (seen) {
            if (goal != Goal.FLEE || !hasPath) {
                val spot = bestSpot(w, true)
                if (spot != null && pathToSpot(w, spot)) { goal = Goal.HIDE; goalSpot = spot } else beginFlee(w)
            }
            return
        }
        if (goal == Goal.FLEE && hasPath) return
        chooseObjective(w, false)
    }

    private fun chooseObjective(w: GameWorld, leaveFirst: Boolean) {
        if (w.escapePhase) {
            if (w.keyComplete) {
                if (leaveFirst && hidden) w.leaveSpot(this)
                if (goal != Goal.EXIT || !hasPath) {
                    if (setPathTo(w.map, w.map.exitApproach.x, w.map.exitApproach.y)) goal = Goal.EXIT
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

    private fun visibleKey(w: GameWorld): KeyFrame? {
        var best: KeyFrame? = null
        var bd = 110f
        for (k in w.keys) {
            if (k.collected) continue
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
        val wantSprint = goal == Goal.FLEE || (danger && goal == Goal.HIDE)
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
                if (k == null || k.collected) { goal = Goal.IDLE; goalKey = null; decideTimer = 0f; return }
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
        }
    }
}
