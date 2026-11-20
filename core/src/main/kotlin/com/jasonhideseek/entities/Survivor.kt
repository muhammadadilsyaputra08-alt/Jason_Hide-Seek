package com.jasonhideseek.entities

import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.KeyFrame

enum class SurvivorStatus { ALIVE, ESCAPED, ELIMINATED }

/** Aksi berdurasi (karakter diam selama animasi). */
enum class SurvivorAction(val frames: String) {
    KEY_PICKUP("key_pickup"), EXIT_INTERACT("exit_interact"), ESCAPING("escape")
}

/** Pemain atau bot. `prefix` = awalan region atlas (player, bot1..bot9). */
abstract class Survivor(val prefix: String, x: Float, y: Float) : Character(x, y) {
    var name: String = prefix
    var status = SurvivorStatus.ALIVE
    /** Key Frame yang sedang dibawa. Hanya pembawa 3 frame yang punya kunci utuh. */
    val heldKeys = ArrayList<KeyFrame>()
    val hasCompleteKey get() = heldKeys.size >= GameConfig.KEY_FRAME_COUNT
    val alive get() = status == SurvivorStatus.ALIVE
    var hidingIn: HidingSpot? = null
    var pickupCooldown = 0f      // jeda sebelum boleh mengambil frame lagi (setelah dibatalkan)
    var hideAnim = 0f            // detik sejak masuk persembunyian
    var stamina = GameConfig.STAMINA_MAX
    var exhausted = false
    var sprinting = false

    var action: SurvivorAction? = null
        private set
    var actionTime = 0f
        private set
    var actionDuration = 0.5f
        private set

    val actionProgress get() = if (action != null && actionDuration > 0f) actionTime / actionDuration else 0f
    val hidden get() = hidingIn != null
    val busy get() = action != null

    fun startAction(a: SurvivorAction, duration: Float) {
        action = a; actionTime = 0f; actionDuration = duration
        moving = false; sprinting = false; clearPath()
    }

    fun clearAction() { action = null }

    /** Maju-kan aksi; mengembalikan aksi yang baru selesai (atau null). */
    fun tickAction(dt: Float): SurvivorAction? {
        val a = action ?: return null
        actionTime += dt
        if (actionTime >= actionDuration) { action = null; return a }
        return null
    }

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
