package com.jasonhideseek.systems

import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.entities.Character
import com.jasonhideseek.entities.Survivor
import com.jasonhideseek.world.MapDefinition

/** Line-of-sight + kerucut pandang Jason. */
class VisionSystem(private val map: MapDefinition) {
    /** Pengubah dinamis dari buff Jason & efek map. */
    var viewMul = 1f
    var proxMul = 1f
    var fovExtra = 0f

    fun canSee(viewer: Character, target: Survivor): Boolean {
        if (!target.alive || target.hidden) return false
        // Pembawa Key Frame lebih mudah terlihat: tiap frame menambah jangkauan pandang.
        return canSeePoint(viewer, target.x, target.y, 1f + GameConfig.CARRIER_SIGHT_BONUS * target.heldKeys.size)
    }

    fun canSeePoint(viewer: Character, tx: Float, ty: Float, scale: Float = 1f): Boolean {
        val dx = tx - viewer.x; val dy = ty - viewer.y
        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (d > GameConfig.JASON_VIEW_RANGE * viewMul * scale) return false
        if (d > GameConfig.JASON_PROXIMITY * proxMul * scale) {
            val cosHalfFov = Math.cos(Math.toRadians(((GameConfig.JASON_FOV_DEG + fovExtra) / 2f).toDouble())).toFloat()
            val fx = viewer.dir.dx; val fy = viewer.dir.dy
            val cos = (dx * fx + dy * fy) / d
            if (cos < cosHalfFov) return false
        }
        return map.lineClear(viewer.x, viewer.y, tx, ty)
    }
}
