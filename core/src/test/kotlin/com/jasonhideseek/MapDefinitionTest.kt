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
        val expected = listOf(31 to 29, 29 to 27, 31 to 29, 31 to 27, 29 to 29)
        for ((i, m) in maps.withIndex()) {
            assertEquals("map ${m.id}: width", expected[i].first, m.tilesW)
            assertEquals("map ${m.id}: height", expected[i].second, m.tilesH)
            assertEquals(m.tilesW, m.tiles[0].size); assertEquals(m.tilesH, m.tiles.size)
            assertTrue("map ${m.id}: hiding spot", m.spotDefs.size >= 8)
            assertTrue("map ${m.id}: key candidates", m.keyCandidates.size >= 12)
        }
    }

    @Test
    fun spawnsKeysSpotsAndExitAreReachable() {
        val path = ArrayList<Vector2>()
        for (m in maps) {
            m.resetChamber(); m.openChamberDoor()   // Jason baru bisa keluar setelah pintu hancur
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
            m.resetChamber()
        }
    }

    @Test
    fun extraCollisionOfVehicleIsApplied() {
        val garage = maps[1]
        // vehicle di (3,3) lebar 2 -> sel (3,3) dan (4,3)
        assertTrue(garage.isSolidTile(3, 9))
        assertTrue(garage.isSolidTile(4, 9))
        // hydraulic lift 64x32 di (7,6) -> sel (7,6) dan (8,6)
        assertTrue(garage.isSolidTile(7, 12)); assertTrue(garage.isSolidTile(8, 12))
        // objek tinggi: windmill 64x64 di (12,10) -> sel (12..13, 9..10)
        val farm = maps[2]
        for (c in 12..13) for (r in 15..16) assertTrue("windmill ($c,$r)", farm.isSolidTile(c, r))
        // boiler 32x64 di (10,16) -> sel (10,16) dan (10,15)
        val bsmt = maps[4]
        assertTrue(bsmt.isSolidTile(10, 22)); assertTrue(bsmt.isSolidTile(10, 21))
    }

    @Test
    fun jasonStartsInsideLockedChamberAndDoorBreaksOpen() {
        val mats = listOf("wood", "shutter", "barn", "steel", "iron")
        val path = ArrayList<Vector2>()
        for ((i, m) in maps.withIndex()) {
            m.resetChamber()
            val a = m.arrival ?: error("map ${m.id}: jason_arrival hilang")
            assertEquals(mats[i], a.material)
            assertEquals(5, a.boundsW); assertEquals(5, a.boundsH)
            assertTrue(a.inChamber(a.spawnCol, a.spawnRow))
            assertTrue("pintu terkunci harus solid", m.isSolidTile(a.doorCol, a.doorRow))
            assertTrue("Jason terkurung", !m.findPath(m.jasonSpawn.x, m.jasonSpawn.y, m.playerSpawn.x, m.playerSpawn.y, path))
            assertTrue("ruang tidak masuk reachable", m.reachableFloorTiles().none { m.inChamber(m.colOf(it.x), m.rowOf(it.y)) })
            m.openChamberDoor()
            assertTrue("setelah pintu hancur Jason bisa keluar", m.findPath(m.jasonSpawn.x, m.jasonSpawn.y, m.playerSpawn.x, m.playerSpawn.y, path))
            m.resetChamber()
            assertTrue(m.isSolidTile(a.doorCol, a.doorRow))
        }
    }

    @Test
    fun arrivalSequenceStagesAreOrdered() {
        val a = maps[0].arrival!!
        val s = com.jasonhideseek.world.ArrivalSequence(a)
        assertEquals(com.jasonhideseek.world.ArrivalStage.LOCKED, s.stage)
        s.update(a.prepDelay + 0.1f); assertEquals(com.jasonhideseek.world.ArrivalStage.WARNING, s.stage)
        s.update(a.warnDuration); assertEquals(com.jasonhideseek.world.ArrivalStage.BREAKING, s.stage)
        s.update(a.breakDuration); assertEquals(com.jasonhideseek.world.ArrivalStage.BROKEN, s.stage)
        assertEquals(20f, a.totalDuration, 0.001f)
    }

    @Test
    fun wallsSolidAndSpawnFree() {
        val m = maps[0]
        assertTrue(m.isSolidTile(0, 0))
        assertTrue(!m.blocked(m.playerSpawn.x, m.playerSpawn.y))
    }
}
