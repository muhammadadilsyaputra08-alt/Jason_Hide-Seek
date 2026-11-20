package com.jasonhideseek.world

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.jasonhideseek.config.GameConfig.TILE
import com.jasonhideseek.game.Assets

/** Merender lapisan tile (Tiles Atlas) + dekorasi lantai/dinding. Objek ber-depth digambar oleh GameScreen. */
class TileMapRenderer(private val map: MapDefinition, private val assets: Assets) {
    private val m = map.index + 1
    private val floors = listOf("a", "b", "c").map { assets.region("tile_map${m}_floor_$it") }
    private val wallTop = assets.region("tile_map${m}_wall_top")
    private val wallFace = assets.region("tile_map${m}_wall_face")
    private val doorH = assets.region("tile_map${m}_door")
    private val doorV = assets.region("tile_map${m}_door_v")
    private val shadow = assets.region("tile_shadow_wall")

    private companion object {
        val ACCENTS = arrayOf(intArrayOf(2), intArrayOf(1), intArrayOf(1, 2), intArrayOf(), intArrayOf(1, 2))
    }

    private fun hash(c: Int, r: Int): Int {
        var h = c * 73856093 xor r * 19349663 xor m * 83492791
        h = h xor (h ushr 13)
        return Math.abs(h)
    }

    fun render(batch: SpriteBatch, left: Float, bottom: Float, right: Float, top: Float) {
        val c0 = Math.max(0, map.colOf(left)); val c1 = Math.min(map.tilesW - 1, map.colOf(right))
        val r0 = Math.max(0, map.rowOf(top)); val r1 = Math.min(map.tilesH - 1, map.rowOf(bottom))
        for (r in r0..r1) for (c in c0..c1) {
            val x = c * TILE.toFloat()
            val y = (map.tilesH - 1 - r) * TILE.toFloat()
            val t = map.tileAt(c, r)
            if (t == 1) {
                batch.draw(if (map.tileAt(c, r + 1) == 1) wallTop else wallFace, x, y)
                continue
            }
            // Lantai bersih: hanya ~10% tile bervariasi (sesuai aksen tiap map), selebihnya polos
            val hh = hash(c, r)
            val acc = ACCENTS[m - 1]
            val floor: TextureRegion = if (acc.isNotEmpty() && hh % 10 == 0) floors[acc[(hh / 10) % acc.size]] else floors[0]
            batch.draw(floor, x, y)
            if (t == 2) batch.draw(doorH, x, y)
            if (t == 3) batch.draw(doorV, x, y)
            // Bayangan di bawah muka dinding
            if (map.tileAt(c, r - 1) == 1 && t == 0) batch.draw(shadow, x, y + TILE - 8f)
        }
    }

    /** Dekorasi layer "floor" (karpet, kabel) dan "wall" (lukisan, jam, pipa). */
    fun renderDecor(batch: SpriteBatch, layer: String) {
        for (p in map.props) {
            if (p.layer != layer) continue
            batch.draw(assets.region(p.item), p.col * TILE.toFloat(), (map.tilesH - 1 - p.row) * TILE.toFloat())
        }
    }
}
