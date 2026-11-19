package com.jasonhideseek

import com.badlogic.gdx.math.Vector2
import com.jasonhideseek.world.MapDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapDefinitionTest {
    private val maps: List<MapDefinition> by lazy {
        val f = listOf("../assets/maps/maps.json", "assets/maps/maps.json").map { File(it) }.first { it.exists() }
        MapDefinition.parseAll(f.readText(Charsets.UTF_8))
    }

    @Test
    fun parsesFiveMapsWith25x17Grid() {
        assertEquals(5, maps.size)
        for (m in maps) {
            assertEquals(25, m.tilesW); assertEquals(17, m.tilesH)
            assertTrue("map ${m.id}: hiding spot", m.spotDefs.size >= 8)
            assertEquals("map ${m.id}: key candidates", 12, m.keyCandidates.size)
        }
    }

    @Test
    fun spawnsKeysSpotsAndExitAreReachable() {
        val path = ArrayList<Vector2>()
        for (m in maps) {
            val p = m.playerSpawn
            assertTrue("map ${m.id}: Jason -> pemain",
                m.findPath(m.jasonSpawn.x, m.jasonSpawn.y, p.x, p.y, path))
            for (k in m.keyCandidates) {
                assertTrue("map ${m.id}: key (${k.col},${k.row}) menabrak collision", !m.isSolidTile(k.col, k.row))
                assertTrue("map ${m.id}: key (${k.col},${k.row}) tak terjangkau", m.findPath(p.x, p.y, k.x, k.y, path))
            }
            for (s in m.spotDefs) {
                assertTrue("map ${m.id}: spot ${s.type.id} tak terjangkau", m.findPath(p.x, p.y, s.x, s.y, path))
            }
            assertTrue("map ${m.id}: exit tak terjangkau", m.findPath(p.x, p.y, m.exitApproach.x, m.exitApproach.y, path))
            assertTrue("map ${m.id}: exit seharusnya dinding/solid", m.isSolidTile(m.exitCol, m.exitRow))
        }
    }

    @Test
    fun extraCollisionOfVehicleIsApplied() {
        val garage = maps[1]
        // vehicle di (2,2) lebar 2 + extra_collision (3,2)
        assertTrue(garage.isSolidTile(2, 2))
        assertTrue(garage.isSolidTile(3, 2))
    }

    @Test
    fun wallsSolidAndSpawnFree() {
        val m = maps[0]
        assertTrue(m.isSolidTile(0, 0))
        assertTrue(!m.blocked(m.playerSpawn.x, m.playerSpawn.y))
    }
}
