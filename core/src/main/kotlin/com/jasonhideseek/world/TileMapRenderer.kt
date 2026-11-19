package com.jasonhideseek.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jasonhideseek.config.GameConfig.TILE
import com.jasonhideseek.game.Assets

class TileMapRenderer(private val map: MapDefinition, assets: Assets) {
    private val m = map.index + 1
    private val floors = listOf("a", "b", "c").map { assets.region("tile_map${m}_floor_$it") }
    private val wallTop = assets.region("tile_map${m}_wall_top")
    private val wallFace = assets.region("tile_map${m}_wall_face")
    private val door = assets.region("tile_map${m}_door")
    private val props = listOf("a", "b").map { assets.region("tile_map${m}_prop_$it") }

    private fun hash(c: Int, r: Int): Int {
        var h = c * 73856093 xor r * 19349663 xor m * 83492791
        h = h xor (h ushr 13)
        return Math.abs(h)
    }

    /** Gambar tile yang terlihat dalam kotak kamera. */
    fun render(batch: SpriteBatch, left: Float, bottom: Float, right: Float, top: Float) {
        val c0 = Math.max(0, map.colOf(left)); val c1 = Math.min(map.tilesW - 1, map.colOf(right))
        val r0 = Math.max(0, map.rowOf(top)); val r1 = Math.min(map.tilesH - 1, map.rowOf(bottom))
        for (r in r0..r1) for (c in c0..c1) {
            val x = c * TILE.toFloat()
            val y = (map.tilesH - 1 - r) * TILE.toFloat()
            val ch = map.charAt(c, r)
            val h = hash(c, r)
            val floor: TextureRegion = when (h % 10) {
                0, 1 -> floors[1]
                2 -> floors[2]
                else -> floors[0]
            }
            when (ch) {
                '#' -> {
                    val below = map.charAt(c, r + 1)
                    batch.draw(if (below == '#') wallTop else wallFace, x, y)
                }
                'D' -> { batch.draw(floor, x, y); batch.draw(door, x, y) }
                'p' -> { batch.draw(floor, x, y); batch.draw(props[h % 2], x, y) }
                else -> batch.draw(floor, x, y)
            }
        }
    }
}
