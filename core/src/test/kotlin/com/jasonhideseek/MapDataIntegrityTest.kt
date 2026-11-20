package com.jasonhideseek

import com.jasonhideseek.world.HidingSpot
import com.jasonhideseek.world.MapDefinition
import com.jasonhideseek.world.SpotState
import com.jasonhideseek.world.SpotType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

/** Konsistensi data: JSON map <-> atlas <-> SpotType <-> hiding_spots.json, dan validasi parser. */
class MapDataIntegrityTest {
    private fun assetFile(p: String): File = listOf("../assets/$p", "assets/$p").map { File(it) }.first { it.exists() }

    private val maps: List<MapDefinition> by lazy { MapDefinition.parseAll(assetFile("maps/maps.json").readText(Charsets.UTF_8)) }

    /** Nama region dari berkas .atlas (baris tanpa indentasi, bukan header/ properti). */
    private fun atlasRegions(file: String): Set<String> = assetFile("atlases/$file").readLines()
        .filter { it.isNotBlank() && !it[0].isWhitespace() && !it.contains(':') && !it.endsWith(".png") }.toSet()

    private val items by lazy { atlasRegions("items.atlas") }
    private val tiles by lazy { atlasRegions("tiles.atlas") }

    @Test
    fun allReferencedSpritesExistInAtlas() {
        for (m in maps) {
            for (n in m.referencedItemRegions) assertTrue("map ${m.id}: region item '$n' tidak ada di items.atlas", n in items)
            for (n in m.referencedTileRegions) assertTrue("map ${m.id}: region tile '$n' tidak ada di tiles.atlas", n in tiles)
            for (s in listOf("a", "b", "c").map { "floor_$it" } + listOf("wall_top", "wall_face", "door", "door_v"))
                assertTrue("map ${m.id}: tile_map${m.id}_$s", "tile_map${m.id}_$s" in tiles)
            for (sp in m.spotDefs) for (st in SpotState.values())
                assertTrue("map ${m.id}: ${HidingSpot.regionName(sp.type, st)}", HidingSpot.regionName(sp.type, st) in items)
        }
        assertTrue("tile_shadow_wall" in tiles)
    }

    @Test
    fun everySpotTypeHasFourSpritesAndCatalogEntry() {
        val catalog = assetFile("hiding_spots.json").readText(Charsets.UTF_8)
        for (t in SpotType.values()) {
            assertTrue("katalog hiding_spots.json memuat ${t.id}", catalog.contains("\"id\": \"${t.id}\""))
            for (st in SpotState.values()) assertTrue(HidingSpot.regionName(t, st), HidingSpot.regionName(t, st) in items)
            assertEquals(t, SpotType.byId(t.id))
        }
        assertEquals(29, SpotType.values().size)
    }

    @Test
    fun newHidingSpotsArePlacedOnTheirMaps() {
        val expected = mapOf(
            0 to setOf(SpotType.BEAR_RUG), 1 to setOf(SpotType.TIRE_STACK, SpotType.MECHANIC_CABINET),
            2 to setOf(SpotType.SILO_GAP, SpotType.HAY_WAGON), 3 to setOf(SpotType.SHRINK_WRAP_PALLET, SpotType.SHIPPING_CONTAINER),
            4 to setOf(SpotType.PIPE_RECESS, SpotType.UTILITY_CABINET))
        for ((i, set) in expected) {
            val got = maps[i].spotDefs.map { it.type }.toSet()
            assertTrue("map ${i + 1}: $set ⊆ $got", got.containsAll(set))
        }
    }

    @Test
    fun noPropOverlapAndBotSpawnsValid() {
        for (m in maps) {
            assertTrue("map ${m.id}: bot spawn candidates", m.botSpawnCandidates.size >= 9)
            for (b in m.botSpawnCandidates) assertTrue("map ${m.id}: bot spawn solid", !m.blocked(b.x, b.y))
            val spotCells = m.spotDefs.map { m.colOf(it.x) to m.rowOf(it.y) }
            assertEquals("map ${m.id}: hiding spot ganda", spotCells.size, spotCells.toSet().size)
            for ((c, r) in spotCells) assertTrue("map ${m.id}: spot ($c,$r) di sel solid", !m.isSolidTile(c, r))
            for (k in m.keyCandidates) assertTrue("map ${m.id}: key di atas hiding spot", (k.col to k.row) !in spotCells)
        }
    }

    // ---- parser: pesan error jelas untuk data rusak ----
    private fun mapJson(tiles: String = "[[1,1,1],[1,0,1],[1,1,1]]", spot: String = "wardrobe", ky: Int = 1, extra: String = "") =
        """{"maps":[{"id":1,"name":"x","width":3,"height":3,"tiles":$tiles,"objects":{
        "spawns":{"player":{"x":1,"y":1},"jason":{"x":1,"y":1}},"exit":{"x":1,"y":2,"orientation":"s"},
        "hiding_spots":[{"x":1,"y":1,"type_id":"$spot"}],"props":[],"decor":[],
        "key_candidates":[{"x":1,"y":$ky}]$extra}}]}"""

    private fun expectError(json: String, contains: String) {
        try { com.jasonhideseek.world.MapDefinition(0, com.badlogic.gdx.utils.JsonReader().parse(json).get("maps").get(0)); fail("seharusnya gagal: $contains") }
        catch (e: IllegalStateException) { assertTrue("pesan '${e.message}' memuat '$contains'", e.message!!.contains(contains)) }
    }

    @Test
    fun parserRejectsBadData() {
        expectError(mapJson(spot = "naga_api"), "tidak dikenal")
        expectError(mapJson(tiles = "[[1,1,1],[1,0],[1,1,1]]"), "panjang")
        expectError(mapJson(tiles = "[[1,1,1],[1,0,1]]"), "jumlah baris")
        expectError(mapJson(tiles = "[[1,1,1],[1,7,1],[1,1,1]]"), "tidak valid")
        expectError(mapJson(ky = 9), "di luar batas")
        expectError(mapJson(ky = 0), "solid")
    }
}
