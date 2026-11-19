package com.jasonhideseek.entities

import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.systems.GameWorld
import java.util.Random

/** Bot penyintas offline: cari tempat sembunyi, panik terhadap Jason/noise, berpindah spot. */
class SurvivorBot(prefix: String, x: Float, y: Float, private val rng: Random) : Survivor(prefix, x, y) {
    private enum class State { SEEK, HIDDEN, FLEE }

    private var state = State.SEEK
    private var target: com.jasonhideseek.world.HidingSpot? = null
    private var thinkTimer = rng.nextFloat() * 0.8f
    private var hiddenFor = 0f
    private var fleeCooldown = 0f
    val nerve = 0.3f + rng.nextFloat() * 0.6f       // makin tinggi makin mudah panik
    private var pendingNoise = 0f
    private var noiseX = 0f
    private var noiseY = 0f

    /** Dipanggil SoundCueSystem saat ada suara kerusakan/pembunuhan. */
    fun hear(nx: Float, ny: Float, radius: Float) {
        if (!alive || dist(nx, ny) > radius) return
        pendingNoise = 1f
        noiseX = nx; noiseY = ny
    }

    fun update(dt: Float, w: GameWorld) {
        animTime += dt
        fleeCooldown -= dt
        if (hidden) { hideAnim += dt; moving = false; sprinting = false; updateStamina(dt) }
        thinkTimer -= dt
        when (state) {
            State.SEEK -> seek(dt, w, speed = GameConfig.BOT_SPEED, sprint = false)
            State.FLEE -> seek(dt, w, speed = GameConfig.BOT_SPRINT, sprint = true)
            State.HIDDEN -> hiddenUpdate(dt, w)
        }
        if (!hidden) updateStamina(dt)
        pendingNoise = 0f
    }

    private fun pickSpot(w: GameWorld, awayFrom: Boolean) {
        val jason = w.jason
        var best: com.jasonhideseek.world.HidingSpot? = null
        var bestScore = Float.MAX_VALUE
        for (s in w.spots) {
            if (!s.usable) continue
            var score = dist(s.x, s.y) + rng.nextFloat() * 90f
            if (awayFrom) {
                val dj = Math.sqrt(((s.x - jason.x) * (s.x - jason.x) + (s.y - jason.y) * (s.y - jason.y)).toDouble()).toFloat()
                if (dj < 110f) continue
                score -= dj * 0.6f
            }
            if (score < bestScore) { bestScore = score; best = s }
        }
        target = best
        if (best != null && !setPathTo(w.map, best.x, best.y - 0f)) target = null
    }

    private fun seek(dt: Float, w: GameWorld, speed: Float, sprint: Boolean) {
        if (hidden) { w.leaveSpot(this) }
        val t = target
        if (t == null || !t.usable) {
            if (thinkTimer <= 0f) { pickSpot(w, state == State.FLEE); thinkTimer = 0.5f }
            moving = false
            return
        }
        sprinting = sprint && !exhausted && stamina > 0f
        val arrived = followPath(dt, if (sprinting) speed else Math.min(speed, GameConfig.BOT_SPEED), w.map)
        if (arrived || dist(t.x, t.y) < 14f) {
            if (w.enterSpot(this, t)) {
                state = State.HIDDEN; hiddenFor = 0f; target = null
            } else {
                target = null
            }
        }
    }

    private fun hiddenUpdate(dt: Float, w: GameWorld) {
        hiddenFor += dt
        moving = false
        if (!w.huntStarted || fleeCooldown > 0f) return
        val spot = hidingIn ?: return
        val dj = w.jason.dist(spot.x, spot.y)
        val jasonTargetsMe = w.jason.targetSpot === spot
        var panic = 0f
        if (pendingNoise > 0f) panic += 0.5f * nerve
        if (dj < 55f && !jasonTargetsMe) panic += 0.35f * nerve * dt * 4f
        if (jasonTargetsMe && dj < 70f) panic += 0.6f * nerve * dt * 3f
        if (panic > 0f && rng.nextFloat() < panic) {
            fleeCooldown = 6f
            state = State.FLEE
            target = null
            thinkTimer = 0f
        }
    }
}
