package com.jasonhideseek

import com.badlogic.gdx.math.Vector2
import com.jasonhideseek.world.MapDefinition
import org.junit.Assert.assertTrue
import org.junit.Test

class MapDefinitionTest {
    @Test
    fun allFiveMapsAreValidAndReachable() {
        for (i in 0 until 5) {
            val m = MapDefinition(i)
            assertTrue("map $i: spot kurang", m.spotDefs.size >= 8)
            assertTrue("map $i: spawn pemain", m.playerSpawn.x > 0f)
            assertTrue("map $i: spawn Jason", m.jasonSpawn.x > 0f)
            val path = ArrayList<Vector2>()
            assertTrue("map $i: Jason tak bisa ke pemain",
                m.findPath(m.jasonSpawn.x, m.jasonSpawn.y, m.playerSpawn.x, m.playerSpawn.y, path))
            for ((_, x, y) in m.spotDefs) {
                assertTrue("map $i: spot tak terjangkau",
                    m.findPath(m.playerSpawn.x, m.playerSpawn.y, x, y, path))
            }
        }
    }

    @Test
    fun wallsAreSolidAndFloorIsNot() {
        val m = MapDefinition(0)
        assertTrue(m.isSolidTile(0, 0))
        assertTrue(!m.blocked(m.playerSpawn.x, m.playerSpawn.y))
    }
}
