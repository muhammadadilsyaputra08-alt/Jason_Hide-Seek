package com.jasonhideseek.systems

import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.entities.Character
import com.jasonhideseek.entities.Survivor
import com.jasonhideseek.world.MapDefinition

/** Line-of-sight + kerucut pandang Jason. */
class VisionSystem(private val map: MapDefinition) {
    private val cosHalfFov = Math.cos(Math.toRadians((GameConfig.JASON_FOV_DEG / 2f).toDouble())).toFloat()

    fun canSee(viewer: Character, target: Survivor): Boolean {
        if (!target.alive || target.hidden) return false
        return canSeePoint(viewer, target.x, target.y)
    }

    fun canSeePoint(viewer: Character, tx: Float, ty: Float): Boolean {
        val dx = tx - viewer.x; val dy = ty - viewer.y
        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (d > GameConfig.JASON_VIEW_RANGE) return false
        if (d > GameConfig.JASON_PROXIMITY) {
            val fx = viewer.dir.dx; val fy = viewer.dir.dy
            val cos = (dx * fx + dy * fy) / d
            if (cos < cosHalfFov) return false
        }
        return map.lineClear(viewer.x, viewer.y, tx, ty)
    }
}
