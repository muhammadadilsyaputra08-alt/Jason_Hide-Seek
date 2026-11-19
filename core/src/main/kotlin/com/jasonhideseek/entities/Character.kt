package com.jasonhideseek.entities

import com.badlogic.gdx.math.Vector2
import com.jasonhideseek.config.Dir
import com.jasonhideseek.world.MapDefinition

/** Basis semua aktor bergerak. (x, y) = titik kaki. */
abstract class Character(var x: Float, var y: Float) {
    var dir = Dir.DOWN
    var animTime = 0f
    var moving = false

    // Pathfinding sederhana (BFS grid)
    val path = ArrayList<Vector2>()
    var pathIdx = 0
    private var stuckTime = 0f

    fun tryMove(dx: Float, dy: Float, map: MapDefinition) {
        if (dx != 0f && !map.blocked(x + dx, y)) x += dx
        if (dy != 0f && !map.blocked(x, y + dy)) y += dy
    }

    fun face(dx: Float, dy: Float) { dir = Dir.from(dx, dy, dir) }

    fun dist(ox: Float, oy: Float): Float {
        val dx = ox - x; val dy = oy - y
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    fun setPathTo(map: MapDefinition, tx: Float, ty: Float): Boolean {
        pathIdx = 0
        stuckTime = 0f
        return map.findPath(x, y, tx, ty, path)
    }

    fun clearPath() { path.clear(); pathIdx = 0 }

    val hasPath get() = pathIdx < path.size

    /** Ikuti path; true jika sudah sampai ujung. */
    fun followPath(dt: Float, speed: Float, map: MapDefinition): Boolean {
        if (!hasPath) { moving = false; return true }
        val t = path[pathIdx]
        val dx = t.x - x; val dy = t.y - y
        val d = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (d < 2.5f) {
            pathIdx++
            return !hasPath
        }
        val step = Math.min(speed * dt, d)
        val ox = x; val oy = y
        tryMove(dx / d * step, dy / d * step, map)
        face(dx, dy)
        moving = true
        val moved = Math.abs(x - ox) + Math.abs(y - oy)
        stuckTime = if (moved < step * 0.2f) stuckTime + dt else 0f
        if (stuckTime > 0.6f) { // tersangkut: snap ke titik berikutnya
            x = t.x; y = t.y; stuckTime = 0f
        }
        return false
    }
}
