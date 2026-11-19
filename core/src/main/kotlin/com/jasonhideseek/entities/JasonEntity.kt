package com.jasonhideseek.entities

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jasonhideseek.ai.Decision
import com.jasonhideseek.ai.JasonDecisionEngine
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.game.Assets
import com.jasonhideseek.systems.GameWorld
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

    val chasing get() = state == JasonState.CHASE

    fun release() { if (state == JasonState.COUNTDOWN_LOCKED) { setState(JasonState.PATROL); patrolWait = 0f } }

    private fun setState(s: JasonState) {
        state = s; stateTime = 0f; checking = false; swinging = false
    }

    fun update(dt: Float, w: GameWorld) {
        animTime += dt
        stateTime += dt
        if (state == JasonState.COUNTDOWN_LOCKED) { moving = false; return }

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
            val d = dist(s.x, s.y)
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

    // ---------- PATROL ----------
    private fun patrol(dt: Float, w: GameWorld) {
        val arrived = followPath(dt, GameConfig.JASON_PATROL_SPEED, w.map)
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
            if (dist(s.x, s.y) > GameConfig.JASON_DETECT_RADIUS) continue
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
        repeat(6) {
            if (rng.nextFloat() < 0.65f && w.spots.isNotEmpty()) {
                val s = w.spots[rng.nextInt(w.spots.size)]
                if (s.state == SpotState.INTACT && s.recheckCooldown <= 0f && setPathTo(w.map, s.x, s.y)) return
            } else {
                val t = w.map.floorTiles[rng.nextInt(w.map.floorTiles.size)]
                if (setPathTo(w.map, t.x, t.y)) return
            }
        }
    }

    // ---------- EVALUATE ----------
    private fun evaluate(w: GameWorld) {
        moving = false
        val s = targetSpot
        if (s == null || s.state != SpotState.INTACT) { setState(JasonState.PATROL); return }
        face(s.x - x, s.y - y)
        if (stateTime < evalDuration) return
        when (engine.decide(s.suspicion, w.huntProgress)) {
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
                followPath(dt, GameConfig.JASON_PATROL_SPEED * 1.2f, w.map)
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
                targetSpot = null
                clearPath()
                setState(JasonState.PATROL)
            }
            return
        }
        if (dist(s.x, s.y) > 20f) {
            if (!hasPath) setPathTo(w.map, s.x, s.y)
            followPath(dt, GameConfig.JASON_PATROL_SPEED * 1.4f, w.map)
            return
        }
        moving = false
        face(s.x - x, s.y - y)
        startSwing(w)
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
                if (t.alive && !t.hidden && dist(t.x, t.y) < GameConfig.JASON_ATTACK_RANGE + 12f) w.killInOpen(t)
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
        if (visible && dist(t.x, t.y) < GameConfig.JASON_ATTACK_RANGE) {
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
        val done = followPath(dt, GameConfig.JASON_CHASE_SPEED, w.map)
        if (done && visible) {
            // path habis (satu tile yang sama): dekati langsung
            val dx = t.x - x; val dy = t.y - y
            val d = Math.max(1f, dist(t.x, t.y))
            tryMove(dx / d * GameConfig.JASON_CHASE_SPEED * dt, dy / d * GameConfig.JASON_CHASE_SPEED * dt, w.map)
            face(dx, dy); moving = true
        }
    }

    // ---------- SEARCH ----------
    private fun search(dt: Float, w: GameWorld) {
        val arrived = followPath(dt, GameConfig.JASON_SEARCH_SPEED, w.map)
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
    private fun startSwing(w: GameWorld) {
        swinging = true; swingTime = 0f; swingHit = false
        w.sound.playAt("slash", x, y, 0.9f)
    }

    private fun tickSwing(dt: Float, w: GameWorld, onHit: () -> Unit): Boolean {
        swingTime += dt
        if (!swingHit && swingTime >= SWING_HIT) {
            swingHit = true
            w.gore.slash(x + dir.dx * 18f, y + 18f + dir.dy * 10f)
            onHit()
        }
        if (swingTime >= SWING_TOTAL) { swinging = false; return true }
        return false
    }

    // ---------- Sprite ----------
    fun currentFrame(assets: Assets): TextureRegion {
        val d = dir.key
        return when {
            swinging -> {
                val f = assets.frames("jason_attack_$d")
                f[Math.min(f.size - 1, (swingTime / (SWING_TOTAL / f.size)).toInt())]
            }
            state == JasonState.EVALUATE_SPOT || (state == JasonState.CHECK_SPOT && checking) ->
                assets.frame("jason_evaluate_$d", stateTime, 6f)
            state == JasonState.CHASE -> assets.frame("jason_chase_$d", animTime, 10f)
            else -> {
                val f = assets.frames("jason_walk_$d")
                if (moving) f[((animTime * 6f).toInt()) % f.size] else f[0]
            }
        }
    }

    companion object {
        const val SWING_HIT = 0.28f
        const val SWING_TOTAL = 0.45f
    }
}
