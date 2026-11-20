package com.jasonhideseek.entities

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jasonhideseek.ai.Decision
import com.jasonhideseek.ai.JasonDecisionEngine
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.game.Assets
import com.jasonhideseek.systems.GameWorld
import com.jasonhideseek.ui.TouchControls
import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.SpotState
import java.util.Random

enum class JasonState { COUNTDOWN_LOCKED, PATROL, EVALUATE_SPOT, CHECK_SPOT, ATTACK_SPOT, CHASE, SEARCH }

class JasonEntity(x: Float, y: Float, private val engine: JasonDecisionEngine, private val rng: Random) : Character(x, y) {
    var state = JasonState.COUNTDOWN_LOCKED
        private set
    var stateTime = 0f
        private set
    var targetSpot: HidingSpot? = null
        private set
    private var chaseTarget: Survivor? = null
    private var lastSeenX = 0f
    private var lastSeenY = 0f
    private var evalDuration = 0.6f
    private var patrolWait = 0f
    private var repathTimer = 0f
    private var loseSight = 0f
    private var searchWait = 0f
    private var checking = false
    private var checkTimer = 0f

    // Ayunan kapak
    private var swinging = false
    private var swingTime = 0f
    private var swingHit = false
    private var swingBreak = false
    private var ended = false
    private var victory = false

    val chasing get() = state == JasonState.CHASE

    // ---- Mode pemain = Jason ----
    var playerControlled = false
    var stamina = GameConfig.STAMINA_MAX
        private set
    private var exhausted = false
    private var sprintingP = false
    private var pTarget: Survivor? = null
    private var pSpot: HidingSpot? = null
    private var huntTimer = 0f
    val isSwinging get() = swinging

    fun release() { if (state == JasonState.COUNTDOWN_LOCKED) { setState(JasonState.PATROL); patrolWait = 0f } }

    private fun setState(s: JasonState) {
        state = s; stateTime = 0f; checking = false; swinging = false
    }

    fun update(dt: Float, w: GameWorld, controls: TouchControls? = null) {
        animTime += dt
        stateTime += dt
        if (state == JasonState.COUNTDOWN_LOCKED) { moving = false; return }
        if (playerControlled && controls != null) { updatePlayer(dt, w, controls); return }
        huntHolder(dt, w)

        if (state == JasonState.PATROL || state == JasonState.EVALUATE_SPOT || state == JasonState.SEARCH) {
            lookForSurvivors(w)
        }

        when (state) {
            JasonState.PATROL -> patrol(dt, w)
            JasonState.EVALUATE_SPOT -> evaluate(w)
            JasonState.CHECK_SPOT -> checkSpot(dt, w)
            JasonState.ATTACK_SPOT -> attackSpot(dt, w)
            JasonState.CHASE -> chase(dt, w)
            JasonState.SEARCH -> search(dt, w)
            JasonState.COUNTDOWN_LOCKED -> {}
        }
    }

    // ---------- Persepsi ----------
    private fun lookForSurvivors(w: GameWorld) {
        var best: Survivor? = null
        var bestD = Float.MAX_VALUE
        for (s in w.survivors) {
            if (!w.vision.canSee(this, s)) continue
            // Pembawa Key Frame diprioritaskan sebagai target kejaran (makin banyak frame, makin utama).
            val d = dist(s.x, s.y) / (1f + 0.6f * s.heldKeys.size + (if (s.hasCompleteKey) 2f else 0f))
            if (d < bestD) { bestD = d; best = s }
        }
        if (best != null) {
            chaseTarget = best
            lastSeenX = best.x; lastSeenY = best.y
            loseSight = 0f; repathTimer = 0f
            clearPath()
            setState(JasonState.CHASE)
            w.sound.playAt("alert", x, y, 0.8f)
        }
    }

    /** Jason mendengar suara di (nx,ny): bila sedang patroli/mencari, ia menyelidiki titik itu. */
    fun hear(nx: Float, ny: Float, radius: Float, chance: Float, w: GameWorld) {
        if (playerControlled || (state != JasonState.PATROL && state != JasonState.SEARCH)) return
        if (dist(nx, ny) > radius || rng.nextFloat() > chance) return
        if (!w.map.findPath(x, y, nx, ny, ArrayList())) return
        lastSeenX = nx; lastSeenY = ny
        setState(JasonState.SEARCH)
        setPathTo(w.map, nx, ny)
        searchWait = 0f
    }

    // ---------- PATROL ----------
    private fun patrol(dt: Float, w: GameWorld) {
        val arrived = followPath(dt, GameConfig.JASON_PATROL_SPEED * w.buffs.patrolMul, w.map)
        if (arrived) {
            moving = false
            patrolWait -= dt
            if (patrolWait <= 0f) {
                pickPatrolTarget(w)
                patrolWait = 0.3f + rng.nextFloat() * 0.8f
            }
        }
        for (s in w.spots) {
            if (s.state != SpotState.INTACT || s.recheckCooldown > 0f) continue
            val carried = s.occupant?.heldKeys?.size ?: 0   // spot yang dihuni pembawa frame lebih terdeteksi
            if (dist(s.x, s.y) > GameConfig.JASON_DETECT_RADIUS * w.buffs.detectMul * (1f + 0.25f * carried)) continue
            if (!w.map.lineClear(x, y, s.x, s.y)) continue
            targetSpot = s
            evalDuration = GameConfig.JASON_EVAL_MIN + rng.nextFloat() * (GameConfig.JASON_EVAL_MAX - GameConfig.JASON_EVAL_MIN)
            moving = false
            face(s.x - x, s.y - y)
            setState(JasonState.EVALUATE_SPOT)
            return
        }
    }

    private fun pickPatrolTarget(w: GameWorld) {
        // Makin banyak frame yang dibawa, makin besar peluang Jason mengendus & menuju pembawanya.
        val carrier = w.keyHolder
        if (carrier != null) {
            val p = Math.min(0.7f, GameConfig.CARRIER_TRACK_CHANCE * carrier.heldKeys.size)
            if (rng.nextFloat() < p && setPathTo(w.map, carrier.x, carrier.y)) return
        }
        repeat(6) {
            if (w.keyComplete && rng.nextFloat() < GameConfig.JASON_EXIT_GUARD_CHANCE) {
                if (setPathTo(w.map, w.map.exitApproach.x, w.map.exitApproach.y)) return
            } else if (rng.nextFloat() < 0.65f && w.spots.isNotEmpty()) {
                val s = w.spots[rng.nextInt(w.spots.size)]
                if (s.state == SpotState.INTACT && s.recheckCooldown <= 0f && setPathTo(w.map, s.x, s.y)) return
            } else {
                val t = w.map.floorTiles[rng.nextInt(w.map.floorTiles.size)]
                if (setPathTo(w.map, t.x, t.y)) return
            }
        }
    }

    /** Kunci utuh sudah terbentuk: Jason memburu pembawanya (menembus persembunyian). */
    private fun huntHolder(dt: Float, w: GameWorld) {
        val h = w.completeKeyHolder ?: return
        if (state != JasonState.PATROL && state != JasonState.SEARCH && state != JasonState.EVALUATE_SPOT) return
        val sp = h.hidingIn
        if (h.hidden && sp != null && sp.state == SpotState.INTACT) {
            targetSpot = sp
            if (setPathTo(w.map, sp.x, sp.y)) setState(JasonState.ATTACK_SPOT)
            return
        }
        huntTimer -= dt
        if (huntTimer > 0f) return
        huntTimer = GameConfig.HUNT_REPATH
        lastSeenX = h.x; lastSeenY = h.y
        if (setPathTo(w.map, h.x, h.y)) { if (state != JasonState.SEARCH) setState(JasonState.SEARCH); searchWait = 0f }
    }

    // ---------- PEMAIN = JASON ----------
    /** Tombol serang: pilih survivor terlihat terdekat, atau spot terdekat; ayunan kapak. */
    fun playerAttack(w: GameWorld) {
        if (swinging || state == JasonState.COUNTDOWN_LOCKED) return
        val reach = GameConfig.JASON_ATTACK_RANGE + w.buffs.attackBonus + 10f
        var t: Survivor? = null; var td = reach
        for (s in w.survivors) {
            if (!s.alive || s.hidden) continue
            val d = dist(s.x, s.y)
            if (d < td) { td = d; t = s }
        }
        var sp: HidingSpot? = null
        if (t == null) {
            var bd = GameConfig.HIDE_RANGE + 6f
            for (s in w.spots) {
                if (s.state != SpotState.INTACT) continue
                val d = dist(s.x, s.y)
                if (d < bd) { bd = d; sp = s }
            }
        }
        pTarget = t; pSpot = sp
        val fx = t?.x ?: sp?.x
        val fy = t?.y ?: sp?.y
        if (fx != null && fy != null) face(fx - x, fy - y)
        moving = false
        startSwing(w, t == null && sp != null)
    }

    private fun updatePlayer(dt: Float, w: GameWorld, c: TouchControls) {
        if (swinging) {
            moving = false; sprintingP = false
            val done = tickSwing(dt, w) {
                val t = pTarget; val sp = pSpot
                if (t != null && t.alive && !t.hidden && dist(t.x, t.y) < GameConfig.JASON_ATTACK_RANGE + w.buffs.attackBonus + 16f) w.killInOpen(t)
                else if (sp != null && sp.state == SpotState.INTACT) w.attackSpot(sp)
            }
            if (done) { pTarget = null; pSpot = null }
            return
        }
        val mx = c.moveX; val my = c.moveY
        val len = Math.sqrt((mx * mx + my * my).toDouble()).toFloat()
        moving = len > 0.15f
        sprintingP = moving && c.sprint && !exhausted && stamina > 0f
        if (moving) {
            val sp = if (sprintingP) GameConfig.JASON_CHASE_SPEED * 1.05f * w.buffs.chaseMul
                     else GameConfig.JASON_PATROL_SPEED * 1.15f * w.buffs.patrolMul
            val k = Math.min(1f, len)
            tryMove(mx / len * sp * k * dt, my / len * sp * k * dt, w.map)
            face(mx, my)
        }
        if (sprintingP) stamina -= 26f * dt else stamina = Math.min(GameConfig.STAMINA_MAX, stamina + 18f * dt)
        if (stamina <= 0f) { stamina = 0f; exhausted = true }
        if (exhausted && stamina >= GameConfig.STAMINA_RECOVER_AT) exhausted = false

        // Status yang dibaca bot: CHASE bila ada survivor terlihat -> bot panik & kabur.
        var seen = false
        for (s in w.survivors) if (w.vision.canSee(this, s)) { seen = true; break }
        val want = if (seen) JasonState.CHASE else JasonState.PATROL
        if (state != want) setState(want)
        var near: HidingSpot? = null; var nd = 42f
        for (s in w.spots) { if (s.state != SpotState.INTACT) continue; val d = dist(s.x, s.y); if (d < nd) { nd = d; near = s } }
        targetSpot = near
    }

    // ---------- EVALUATE ----------
    private fun evaluate(w: GameWorld) {
        moving = false
        val s = targetSpot
        if (s == null || s.state != SpotState.INTACT) { setState(JasonState.PATROL); return }
        face(s.x - x, s.y - y)
        if (stateTime < evalDuration) return
        when (engine.decide(s.suspicion + w.buffs.suspicionBonus + (s.occupant?.heldKeys?.size ?: 0) * GameConfig.CARRIER_SPOT_SUSPICION, w.huntProgress)) {
            Decision.SKIP -> {
                s.recheckCooldown = GameConfig.SPOT_RECHECK_COOLDOWN
                targetSpot = null
                setState(JasonState.PATROL)
            }
            Decision.CHECK -> { setPathTo(w.map, s.x, s.y); setState(JasonState.CHECK_SPOT) }
            Decision.ATTACK -> { setPathTo(w.map, s.x, s.y); setState(JasonState.ATTACK_SPOT) }
        }
    }

    // ---------- CHECK ----------
    private fun checkSpot(dt: Float, w: GameWorld) {
        val s = targetSpot
        if (s == null || (s.state != SpotState.INTACT && s.state != SpotState.BEING_CHECKED)) { setState(JasonState.PATROL); return }
        if (!checking) {
            if (dist(s.x, s.y) > 18f) {
                if (!hasPath) setPathTo(w.map, s.x, s.y)
                followPath(dt, GameConfig.JASON_PATROL_SPEED * 1.2f * w.buffs.patrolMul, w.map)
                return
            }
            checking = true; checkTimer = 0f; moving = false
            face(s.x - x, s.y - y)
            s.state = SpotState.BEING_CHECKED
            w.sound.playAt("creak", s.x, s.y, 0.8f)
        }
        checkTimer += dt
        if (checkTimer >= GameConfig.SPOT_CHECK_DURATION) {
            w.resolveCheck(s)
            s.recheckCooldown = GameConfig.SPOT_RECHECK_COOLDOWN
            targetSpot = null
            clearPath()
            setState(JasonState.PATROL)
        }
    }

    // ---------- ATTACK SPOT ----------
    private fun attackSpot(dt: Float, w: GameWorld) {
        val s = targetSpot
        if (s == null || s.state != SpotState.INTACT) { setState(JasonState.PATROL); return }
        if (swinging) {
            if (tickSwing(dt, w) { w.attackSpot(s) }) {
                // Spot berpenghuni yang belum hancur (masih punya ketahanan): hantam lagi.
                if (!(s.state == SpotState.INTACT && s.occupant != null)) {
                    targetSpot = null
                    clearPath()
                    setState(JasonState.PATROL)
                }
            }
            return
        }
        if (dist(s.x, s.y) > 20f) {
            if (!hasPath) setPathTo(w.map, s.x, s.y)
            followPath(dt, GameConfig.JASON_PATROL_SPEED * 1.4f * w.buffs.patrolMul, w.map)
            return
        }
        moving = false
        face(s.x - x, s.y - y)
        startSwing(w, true)
    }

    // ---------- CHASE ----------
    private fun chase(dt: Float, w: GameWorld) {
        val t = chaseTarget
        if (t == null || !t.alive) {
            if (swinging) { swinging = false }
            clearPath(); setState(JasonState.PATROL); return
        }
        if (swinging) {
            val done = tickSwing(dt, w) {
                if (t.alive && !t.hidden && dist(t.x, t.y) < (GameConfig.JASON_ATTACK_RANGE + w.buffs.attackBonus) + 12f) w.killInOpen(t)
            }
            if (done) { clearPath(); repathTimer = 0f }
            return
        }
        val visible = w.vision.canSee(this, t)
        if (visible) { lastSeenX = t.x; lastSeenY = t.y; loseSight = 0f } else loseSight += dt
        if (loseSight > GameConfig.LOSE_SIGHT_TIME) {
            w.onLostSight(lastSeenX, lastSeenY)
            clearPath()
            setPathTo(w.map, lastSeenX, lastSeenY)
            searchWait = 0f
            setState(JasonState.SEARCH)
            return
        }
        if (visible && dist(t.x, t.y) < (GameConfig.JASON_ATTACK_RANGE + w.buffs.attackBonus)) {
            moving = false
            face(t.x - x, t.y - y)
            startSwing(w)
            return
        }
        repathTimer -= dt
        if (repathTimer <= 0f || !hasPath) {
            setPathTo(w.map, if (visible) t.x else lastSeenX, if (visible) t.y else lastSeenY)
            repathTimer = 0.25f
        }
        val done = followPath(dt, (GameConfig.JASON_CHASE_SPEED * w.buffs.chaseMul), w.map)
        if (done && visible) {
            // path habis (satu tile yang sama): dekati langsung
            val dx = t.x - x; val dy = t.y - y
            val d = Math.max(1f, dist(t.x, t.y))
            tryMove(dx / d * (GameConfig.JASON_CHASE_SPEED * w.buffs.chaseMul) * dt, dy / d * (GameConfig.JASON_CHASE_SPEED * w.buffs.chaseMul) * dt, w.map)
            face(dx, dy); moving = true
        }
    }

    // ---------- SEARCH ----------
    private fun search(dt: Float, w: GameWorld) {
        val arrived = followPath(dt, GameConfig.JASON_SEARCH_SPEED * w.buffs.patrolMul, w.map)
        if (!arrived) return
        moving = false
        searchWait += dt
        if (searchWait < 1.5f) return
        // Setelah celingak-celinguk, periksa spot terdekat dari titik terakhir terlihat.
        var near: HidingSpot? = null
        var nd = 96f
        for (s in w.spots) {
            if (s.state != SpotState.INTACT) continue
            val d = Math.sqrt(((s.x - lastSeenX) * (s.x - lastSeenX) + (s.y - lastSeenY) * (s.y - lastSeenY)).toDouble()).toFloat()
            if (d < nd) { nd = d; near = s }
        }
        clearPath()
        setState(JasonState.PATROL)
        if (near != null) {
            near.suspicion = Math.min(1f, near.suspicion + 0.6f)
            near.recheckCooldown = 0f
            setPathTo(w.map, near.x, near.y)
        }
    }

    // ---------- Ayunan ----------
    private fun startSwing(w: GameWorld, breaking: Boolean = false) {
        swinging = true; swingTime = 0f; swingHit = false; swingBreak = breaking
        w.sound.playAt("slash", x, y, 0.9f)
    }

    private fun tickSwing(dt: Float, w: GameWorld, onHit: () -> Unit): Boolean {
        swingTime += dt
        val hitAt = if (swingBreak) BREAK_HIT else SWING_HIT
        val total = if (swingBreak) BREAK_TOTAL else SWING_TOTAL
        if (!swingHit && swingTime >= hitAt) {
            swingHit = true
            w.gore.slash(x + dir.dx * 18f, y + 18f + dir.dy * 10f)
            onHit()
        }
        if (swingTime >= total) { swinging = false; return true }
        return false
    }

    // ---------- Akhir pertandingan ----------
    fun setEnded(win: Boolean) { ended = true; victory = win; swinging = false; moving = false; animTime = 0f }

    fun updateEnded(dt: Float) { animTime += dt }

    // ---------- Sprite (frame 64x64, pivot kaki 32,51) ----------
    fun currentFrame(assets: Assets): TextureRegion {
        val d = dir.key
        return when {
            ended && victory -> assets.frame("jason_victory", animTime, 6f)
            ended -> assets.frame("jason_idle_$d", animTime, 2f)
            swinging && swingBreak -> {
                val f = assets.frames("jason_break_$d")
                f[Math.min(f.size - 1, (swingTime / (BREAK_TOTAL / f.size)).toInt())]
            }
            swinging -> {
                val f = assets.frames("jason_attack_$d")
                f[Math.min(f.size - 1, (swingTime / (SWING_TOTAL / f.size)).toInt())]
            }
            state == JasonState.EVALUATE_SPOT || (state == JasonState.CHECK_SPOT && checking) ->
                assets.frame("jason_evaluate_$d", stateTime, 6f)
            (state == JasonState.CHASE && !playerControlled) || sprintingP -> assets.frame("jason_chase_$d", animTime, 10f)
            moving -> assets.frame("jason_walk_$d", animTime, 6f)
            else -> assets.frame("jason_idle_$d", animTime, 2f)
        }
    }

    companion object {
        const val SWING_HIT = 0.28f
        const val SWING_TOTAL = 0.45f
        const val BREAK_HIT = 0.36f
        const val BREAK_TOTAL = 0.6f
    }
}
