package com.jasonhideseek.world

/** Satu Key Frame di map. `number` 1..3 menentukan sprite key_frame_N. */
class KeyFrame(val number: Int, val x: Float, val y: Float) {
    var collected = false
}

enum class ExitState { LOCKED, UNLOCKING, OPEN }
