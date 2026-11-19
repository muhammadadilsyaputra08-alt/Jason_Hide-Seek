package com.jasonhideseek.world

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.JsonValue
import com.jasonhideseek.config.GameConfig.TILE
import java.util.ArrayDeque

/** Prop / dekorasi dari layer objek maps.json (x,y = sel grid, origin kiri-atas). */
class PropDef(val item: String, val col: Int, val row: Int, val wCells: Int, val layer: String)

class SpotDef(val type: SpotType, val x: Float, val y: Float)

class KeyCandidate(val col: Int, val row: Int, val x: Float, val y: Float)

enum class ExitOrientation { SOUTH, EAST, NORTH, WEST;
    val suffix get() = if (this == EAST || this == WEST) "e" else "s" }

/**
 * Satu map hasil parsing assets/maps/maps.json (schema v2):
 * Tile Data (grid), Object Data (props, hiding spot), Spawn Data, Objective Data (key candidates), Exit Data.
 */
class MapDefinition(val index: Int, json: JsonValue) {
    val id = json.getInt("id")
    val name: String = json.getString("name")
    val tilesW = json.getInt("width")
    val tilesH = json.getInt("height")
    val widthPx = tilesW * TILE.toFloat()
    val heightPx = tilesH * TILE.toFloat()

    /** tiles[row][col]: 0 floor, 1 wall, 2 door_h, 3 door_v */
    val tiles: Array<IntArray> = Array(tilesH) { r ->
        val row = json.get("tiles").get(r)
        IntArray(tilesW) { c -> row.getInt(c) }
    }
    private val solid = Array(tilesH) { r -> BooleanArray(tilesW) { c -> tiles[r][c] == 1 } }

    val playerSpawn = Vector2()
    val jasonSpawn = Vector2()
    val spotDefs = ArrayList<SpotDef>()
    val props = ArrayList<PropDef>()
    val keyCandidates = ArrayList<KeyCandidate>()
    val exitCol: Int
    val exitRow: Int
    val exitOrientation: ExitOrientation
    /** Titik berdiri di dalam map tepat di depan pintu keluar. */
    val exitApproach = Vector2()
    val exitCenter = Vector2()
    val floorTiles = ArrayList<Vector2>()

    init {
        val o = json.get("objects")
        val sp = o.get("spawns")
        playerSpawn.set(cellCenter(sp.get("player").getInt("x"), sp.get("player").getInt("y")))
        jasonSpawn.set(cellCenter(sp.get("jason").getInt("x"), sp.get("jason").getInt("y")))

        for (h in o.get("hiding_spots")) {
            val c = cellCenter(h.getInt("x"), h.getInt("y"))
            spotDefs.add(SpotDef(SpotType.byId(h.getString("type_id")), c.x, c.y))
        }
        for (p in o.get("props")) {
            val w = p.getInt("w", 1); val hh = p.getInt("h", 1)
            val col = p.getInt("x"); val row = p.getInt("y")
            props.add(PropDef(p.getString("item"), col, row, w, "object"))
            if (p.getBoolean("collision", false)) {
                for (dx in 0 until w) for (dy in 0 until hh) markSolid(col + dx, row + dy)
                val extra = p.get("extra_collision")
                if (extra != null) for (e in extra) markSolid(e.getInt("x"), e.getInt("y"))
            }
        }
        for (d in o.get("decor")) {
            props.add(PropDef(d.getString("item"), d.getInt("x"), d.getInt("y"), d.getInt("w", 1), d.getString("layer", "object")))
            if (d.getBoolean("collision", false)) markSolid(d.getInt("x"), d.getInt("y"))
        }
        for (k in o.get("key_candidates")) {
            val c = cellCenter(k.getInt("x"), k.getInt("y"))
            keyCandidates.add(KeyCandidate(k.getInt("x"), k.getInt("y"), c.x, c.y))
        }
        val ex = o.get("exit")
        exitCol = ex.getInt("x"); exitRow = ex.getInt("y")
        exitOrientation = when (ex.getString("orientation", "s")) {
            "e" -> ExitOrientation.EAST
            "w" -> ExitOrientation.WEST
            "n" -> ExitOrientation.NORTH
            else -> ExitOrientation.SOUTH
        }
        exitCenter.set(cellCenter(exitCol, exitRow))
        val (ac, ar) = when (exitOrientation) {
            ExitOrientation.SOUTH -> exitCol to exitRow - 1
            ExitOrientation.NORTH -> exitCol to exitRow + 1
            ExitOrientation.EAST -> exitCol - 1 to exitRow
            ExitOrientation.WEST -> exitCol + 1 to exitRow
        }
        exitApproach.set(cellCenter(ac, ar))

        for (r in 0 until tilesH) for (c in 0 until tilesW) {
            if (!solid[r][c]) floorTiles.add(cellCenter(c, r))
        }
    }

    private fun markSolid(c: Int, r: Int) {
        if (c in 0 until tilesW && r in 0 until tilesH) solid[r][c] = true
    }

    /** Pusat sel grid (col,row dari kiri-atas) dalam koordinat dunia (y ke atas). */
    fun cellCenter(col: Int, row: Int) =
        Vector2((col + 0.5f) * TILE, (tilesH - 1 - row + 0.5f) * TILE)

    fun tileAt(col: Int, row: Int): Int =
        if (col < 0 || row < 0 || col >= tilesW || row >= tilesH) 1 else tiles[row][col]

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

    /** BFS 4-arah pada grid; hasil = pusat tile (tanpa titik awal). */
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

    fun tileCenter(col: Int, row: Int) = cellCenter(col, row)

    companion object {
        val NAMES = listOf(
            "Aula Rumah Tua", "Garasi & Bagasi", "Ladang & Lumbung", "Gudang Logistik", "Ruang Bawah Tanah"
        )

        /** Parse seluruh maps.json (teks) menjadi 5 MapDefinition. */
        fun parseAll(text: String): List<MapDefinition> {
            val root = JsonReader().parse(text)
            val out = ArrayList<MapDefinition>()
            var i = 0
            for (m in root.get("maps")) out.add(MapDefinition(i++, m))
            return out
        }
    }
}
