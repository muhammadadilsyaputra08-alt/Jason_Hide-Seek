package com.jasonhideseek.entities

import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.world.HidingSpot

/** Pemain atau bot. `prefix` = awalan region atlas (player, bot1..bot4). */
abstract class Survivor(val prefix: String, x: Float, y: Float) : Character(x, y) {
    var alive = true
    var hidingIn: HidingSpot? = null
    var hideAnim = 0f            // detik sejak masuk persembunyian
    var stamina = GameConfig.STAMINA_MAX
    var exhausted = false
    var sprinting = false

    val hidden get() = hidingIn != null

    fun updateStamina(dt: Float) {
        if (sprinting && moving) {
            stamina -= GameConfig.STAMINA_DRAIN * dt
            if (stamina <= 0f) { stamina = 0f; exhausted = true; sprinting = false }
        } else {
            stamina = Math.min(GameConfig.STAMINA_MAX, stamina + GameConfig.STAMINA_REGEN * dt)
            if (exhausted && stamina >= GameConfig.STAMINA_RECOVER_AT) exhausted = false
        }
    }
}
