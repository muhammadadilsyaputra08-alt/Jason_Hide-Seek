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
            val ov = map.tileOverrideAt(c, r)
            if (ov != null) {
                // Sprite tile struktural/lantai khusus (jendela, shutter, crop rows, conveyor, ...)
                batch.draw(overrideRegion(ov), x, y)
                if (t == 0 && map.tileAt(c, r - 1) == 1) batch.draw(shadow, x, y + TILE - 8f)
                continue
            }
            val ar = map.arrival
            if (ar != null && map.chamberDoorOpen && c == ar.doorCol && r == ar.doorRow) {
                batch.draw(floors[0], x, y); continue   // pintu hancur: lantai terbuka (debris digambar renderArrivalDoor)
            }
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

    private val overrideCache = HashMap<String, TextureRegion>()
    private fun overrideRegion(name: String) = overrideCache.getOrPut(name) { assets.region(name) }

    /**
     * Dekorasi layer "floor" (karpet, kabel), "fx" (efek animasi, mis. kebocoran air), "wall" (lukisan, jam, pipa)
     * dan "ceiling" (chandelier/lampu gantung; digambar di atas karakter).
     * Region beranimasi (lebih dari satu frame bernama sama) diputar 6 fps; sprite lebih sempit dari 1 sel diratakan di tengah.
     */
    /** Sprite pintu ruang kedatangan sesuai tahap (getar saat WARNING/BREAKING; debris permanen setelah hancur). */
    fun renderArrivalDoor(batch: SpriteBatch, seq: ArrivalSequence?, time: Float) {
        val d = map.arrival ?: return
        val name = seq?.doorRegion() ?: "door_arrival_${d.material}_closed"
        val reg = assets.region(name)
        val x = d.doorCol * TILE.toFloat() + (TILE - reg.regionWidth) / 2f + (seq?.shakeX(time) ?: 0f)
        batch.draw(reg, x, (map.tilesH - 1 - d.doorRow) * TILE.toFloat())
    }

    fun renderDecor(batch: SpriteBatch, layer: String, time: Float = 0f, lampOn: Boolean = true) {
        for (p in map.props) {
            if (p.layer != layer) continue
            val frames = assets.framesOrNull(p.item)
            val reg = if (layer == "arrival_lamp" && frames != null && frames.size > 1) frames[if (lampOn) 0 else 1]
                      else if (frames != null && frames.size > 1) frames[((time * 6f).toInt() + p.col * 3 + p.row) % frames.size]
                      else assets.region(p.item)
            val offX = if (reg.regionWidth < TILE) (TILE - reg.regionWidth) / 2f else 0f
            batch.draw(reg, p.col * TILE.toFloat() + offX, (map.tilesH - 1 - p.row) * TILE.toFloat())
        }
    }
}
