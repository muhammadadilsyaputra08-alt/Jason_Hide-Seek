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
    fun parsesFiveMapsWithOwnGridSizes() {
        assertEquals(5, maps.size)
        val expected = listOf(31 to 23, 29 to 21, 31 to 23, 31 to 21, 29 to 23)
        for ((i, m) in maps.withIndex()) {
            assertEquals("map ${m.id}: width", expected[i].first, m.tilesW)
            assertEquals("map ${m.id}: height", expected[i].second, m.tilesH)
            assertEquals(m.tilesW, m.tiles[0].size); assertEquals(m.tilesH, m.tiles.size)
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
        // vehicle di (3,3) lebar 2 -> sel (3,3) dan (4,3)
        assertTrue(garage.isSolidTile(3, 3))
        assertTrue(garage.isSolidTile(4, 3))
        // hydraulic lift 64x32 di (7,6) -> sel (7,6) dan (8,6)
        assertTrue(garage.isSolidTile(7, 6)); assertTrue(garage.isSolidTile(8, 6))
        // objek tinggi: windmill 64x64 di (12,10) -> sel (12..13, 9..10)
        val farm = maps[2]
        for (c in 12..13) for (r in 9..10) assertTrue("windmill ($c,$r)", farm.isSolidTile(c, r))
        // boiler 32x64 di (10,16) -> sel (10,16) dan (10,15)
        val bsmt = maps[4]
        assertTrue(bsmt.isSolidTile(10, 16)); assertTrue(bsmt.isSolidTile(10, 15))
    }

    @Test
    fun wallsSolidAndSpawnFree() {
        val m = maps[0]
        assertTrue(m.isSolidTile(0, 0))
        assertTrue(!m.blocked(m.playerSpawn.x, m.playerSpawn.y))
    }
}
