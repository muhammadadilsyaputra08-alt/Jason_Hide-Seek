package com.jasonhideseek.world

import com.jasonhideseek.entities.Survivor

/**
 * Satu Key Frame di map. `number` 1..3 menentukan sprite key_frame_N.
 * `holder` = survivor yang membawanya; `claimer` = survivor yang sedang mengambilnya (channeling).
 */
class KeyFrame(val number: Int, var x: Float, var y: Float) {
    var holder: Survivor? = null
    var claimer: Survivor? = null
    val collected get() = holder != null
    val available get() = holder == null && claimer == null
}

enum class ExitState { LOCKED, UNLOCKING, OPEN }
