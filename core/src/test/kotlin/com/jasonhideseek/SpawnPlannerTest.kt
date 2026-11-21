package com.jasonhideseek

import com.jasonhideseek.systems.SpawnPlanner
import com.jasonhideseek.world.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Random

class SpawnPlannerTest {
    private val maps: List<MapDefinition> by lazy {
        val f = listOf("../assets/maps/maps.json", "assets/maps/maps.json").map { File(it) }.first { it.exists() }
        MapDefinition.parseAll(f.readText(Charsets.UTF_8))
    }

    @Test
    fun spawnsAreRandomReachableAndSeparatedFromJason() {
        for (m in maps) {
            val reach = m.reachableFloorTiles().toSet()
            val firsts = HashSet<String>()
            repeat(20) { seed ->
                val plan = SpawnPlanner.plan(m, Random(seed.toLong()), 10)
                assertEquals(10, plan.survivors.size)
                assertTrue("map ${m.id}: Jason harus di ruang kedatangan", m.arrival!!.inChamber(m.colOf(plan.jason.x), m.rowOf(plan.jason.y)))
                plan.survivors.forEach { assertTrue("map ${m.id}: reachable", it in reach) }
                assertTrue(plan.survivors.map { it.x to it.y }.toSet().size == 10)
                firsts.add("${plan.jason.x},${plan.jason.y}")
            }
            assertEquals("map ${m.id}: Jason spawn tetap (bukan acak)", 1, firsts.size)
        }
    }
}
