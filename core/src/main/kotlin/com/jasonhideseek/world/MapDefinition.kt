package com.jasonhideseek.world

import com.badlogic.gdx.math.Vector2
import com.jasonhideseek.config.GameConfig.TILE
import java.util.ArrayDeque

class MapDefinition(val index: Int) {
    val rows: Array<String> = MapData.LAYOUTS[index]
    val tilesW = rows[0].length
    val tilesH = rows.size
    val widthPx = tilesW * TILE.toFloat()
    val heightPx = tilesH * TILE.toFloat()

    val name = NAMES[index]

    val playerSpawn = Vector2()
    val jasonSpawn = Vector2()
    val spotDefs = ArrayList<Triple<SpotType, Float, Float>>()
    val floorTiles = ArrayList<Vector2>()      // pusat tile yang bisa dilewati
    private val solid = Array(tilesH) { BooleanArray(tilesW) }

    init {
        val catalog = SpotType.BY_MAP[index]
        for (row in 0 until tilesH) for (col in 0 until tilesW) {
            val ch = charAt(col, row)
            val cx = (col + 0.5f) * TILE
            val cy = (tilesH - 1 - row + 0.5f) * TILE
            when {
                ch == '#' || ch == 'p' -> solid[row][col] = true
                ch == 'P' -> playerSpawn.set(cx, cy)
                ch == 'J' -> jasonSpawn.set(cx, cy)
                ch in '1'..'4' -> spotDefs.add(Triple(catalog[ch - '1'], cx, cy))
            }
            if (!solid[row][col]) floorTiles.add(Vector2(cx, cy))
        }
    }

    fun charAt(col: Int, row: Int): Char =
        if (col < 0 || row < 0 || col >= tilesW || row >= tilesH) '#' else rows[row][col]

    // Konversi koordinat tile (ty = indeks baris) <-> dunia (y ke atas)
    fun isSolidTile(col: Int, row: Int) =
        col < 0 || row < 0 || col >= tilesW || row >= tilesH || solid[row][col]

    fun colOf(x: Float) = Math.floor((x / TILE).toDouble()).toInt()
    fun rowOf(y: Float) = tilesH - 1 - Math.floor((y / TILE).toDouble()).toInt()

    fun isSolidAt(x: Float, y: Float) = isSolidTile(colOf(x), rowOf(y))

    /** Kotak kaki karakter: setengah lebar 5, tinggi 6. */
    fun blocked(x: Float, y: Float): Boolean =
        isSolidAt(x - 5f, y) || isSolidAt(x + 5f, y) || isSolidAt(x - 5f, y + 6f) || isSolidAt(x + 5f, y + 6f)

    /** Line-of-sight: tidak ada dinding/prop di sepanjang garis. */
    fun lineClear(x0: Float, y0: Float, x1: Float, y1: Float): Boolean {
        val dx = x1 - x0; val dy = y1 - y0
        val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        val steps = (dist / 6f).toInt() + 1
        for (i in 1 until steps) {
            val t = i.toFloat() / steps
            if (isSolidAt(x0 + dx * t, y0 + dy * t + 6f)) return false
        }
        return true
    }

    /** BFS 4-arah pada grid tile; mengembalikan titik-titik pusat tile (tanpa titik awal). */
    fun findPath(fx: Float, fy: Float, tx: Float, ty: Float, out: ArrayList<Vector2>): Boolean {
        out.clear()
        val sc = colOf(fx); val sr = rowOf(fy)
        val ec = colOf(tx); val er = rowOf(ty)
        if (isSolidTile(ec, er) || isSolidTile(sc, sr)) return false
        if (sc == ec && sr == er) { out.add(tileCenter(ec, er)); return true }
        val prev = IntArray(tilesW * tilesH) { -1 }
        val q = ArrayDeque<Int>()
        val s = sr * tilesW + sc
        prev[s] = s
        q.add(s)
        val goal = er * tilesW + ec
        val dc = intArrayOf(1, -1, 0, 0)
        val dr = intArrayOf(0, 0, 1, -1)
        while (!q.isEmpty()) {
            val cur = q.poll()
            if (cur == goal) break
            val c = cur % tilesW; val r = cur / tilesW
            for (k in 0 until 4) {
                val nc = c + dc[k]; val nr = r + dr[k]
                if (isSolidTile(nc, nr)) continue
                val ni = nr * tilesW + nc
                if (prev[ni] != -1) continue
                prev[ni] = cur
                q.add(ni)
            }
        }
        if (prev[goal] == -1) return false
        var cur = goal
        while (cur != s) {
            out.add(tileCenter(cur % tilesW, cur / tilesW))
            cur = prev[cur]
        }
        out.reverse()
        return true
    }

    fun tileCenter(col: Int, row: Int) =
        Vector2((col + 0.5f) * TILE, (tilesH - 1 - row + 0.5f) * TILE)

    companion object {
        val NAMES = listOf(
            "Aula Rumah Tua", "Garasi & Bagasi", "Ladang & Lumbung", "Gudang Logistik", "Ruang Bawah Tanah"
        )
    }
}
