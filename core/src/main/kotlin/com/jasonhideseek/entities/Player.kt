package com.jasonhideseek.entities

import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.ui.TouchControls
import com.jasonhideseek.world.MapDefinition

class Player(x: Float, y: Float) : Survivor("player", x, y) {
    fun update(dt: Float, controls: TouchControls, map: MapDefinition) {
        animTime += dt
        if (hidden) { hideAnim += dt; moving = false; sprinting = false; updateStamina(dt); return }
        val mx = controls.moveX; val my = controls.moveY
        val len = Math.sqrt((mx * mx + my * my).toDouble()).toFloat()
        moving = len > 0.15f
        sprinting = moving && controls.sprint && !exhausted && stamina > 0f
        if (moving) {
            val speed = if (sprinting) GameConfig.PLAYER_SPRINT else GameConfig.PLAYER_SPEED
            val k = Math.min(1f, len)
            tryMove(mx / len * speed * k * dt, my / len * speed * k * dt, map)
            face(mx, my)
        }
        updateStamina(dt)
    }
}
