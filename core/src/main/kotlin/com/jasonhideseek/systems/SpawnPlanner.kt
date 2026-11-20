package com.jasonhideseek.systems

import com.badlogic.gdx.math.Vector2
import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.world.MapDefinition
import java.util.Random

class SpawnPlan(val survivors: List<Vector2>, val jason: Vector2)

/** Titik spawn acak: Jason dan semua survivor, selalu di lantai yang terhubung ke pintu keluar. */
object SpawnPlanner {
    fun plan(map: MapDefinition, rng: Random, survivorCount: Int): SpawnPlan {
        val pool = map.reachableFloorTiles().filter { t ->
            map.spotDefs.none { Math.hypot((t.x - it.x).toDouble(), (t.y - it.y).toDouble()) < 8.0 } &&
                Math.hypot((t.x - map.exitApproach.x).toDouble(), (t.y - map.exitApproach.y).toDouble()) > 40.0
        }.toMutableList()
        pool.shuffle(rng)
        val jason = pool[0]
        val chosen = ArrayList<Vector2>()
        var safe = GameConfig.JASON_SAFE_SPAWN
        var gap = GameConfig.SURVIVOR_SPAWN_GAP
        var round = 0
        while (chosen.size < survivorCount && round < 8) {
            for (t in pool) {
                if (chosen.size >= survivorCount) break
                if (t === jason || chosen.any { it === t }) continue
                if (dist(t, jason) < safe) continue
                if (chosen.any { dist(it, t) < gap }) continue
                chosen.add(t)
            }
            safe *= 0.75f; gap *= 0.75f; round++
        }
        var i = 0
        while (chosen.size < survivorCount) chosen.add(pool[i++ % pool.size])
        chosen.shuffle(rng)
        return SpawnPlan(chosen, jason)
    }

    private fun dist(a: Vector2, b: Vector2) = Math.hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()
}
