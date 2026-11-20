package com.jasonhideseek.world

import com.jasonhideseek.config.GameConfig
import com.jasonhideseek.entities.Survivor

enum class Material(val debris: String) {
    WOOD("fx_debris_wood"), METAL("fx_debris_metal"), CARD("fx_debris_card"),
    HAY("fx_debris_hay"), CLOTH("fx_debris_card")
}

/** Katalog tipe objek sembunyi (sesuai assets/hiding_spots.json). */
enum class SpotType(val id: String, val material: Material) {
    WARDROBE("wardrobe", Material.WOOD), UNDER_BED("under_bed", Material.WOOD),
    HEAVY_CURTAIN("heavy_curtain", Material.CLOTH), HALL_CHEST("hall_chest", Material.WOOD),
    OIL_BARREL("oil_barrel", Material.METAL), CAR_TRUNK("car_trunk", Material.METAL),
    GARAGE_CRATE("garage_crate", Material.WOOD), UNDER_WORKBENCH("under_workbench", Material.WOOD),
    HAY_STACK("hay_stack", Material.HAY), HARVEST_CRATE("harvest_crate", Material.WOOD),
    FARM_BOX("farm_box", Material.CARD), WATER_BARREL("water_barrel", Material.WOOD),
    LARGE_CARDBOARD("large_cardboard", Material.CARD), CARGO_CRATE("cargo_crate", Material.METAL),
    DRUM_BARREL("drum_barrel", Material.METAL), PALLET_GAP("pallet_gap", Material.WOOD),
    METAL_LOCKER("metal_locker", Material.METAL), UNDER_COT("under_cot", Material.CLOTH),
    TARP_CURTAIN("tarp_curtain", Material.CLOTH), BASEMENT_DRUM("basement_drum", Material.METAL);

    companion object {
        private val byId = values().associateBy { it.id }
        fun byId(id: String): SpotType = byId[id] ?: error("Tipe hiding spot tidak dikenal: $id")
    }
}

enum class SpotState { INTACT, BEING_CHECKED, BROKEN_CLEAN, BROKEN_BLOODY }

/** Titik sembunyi, kapasitas 1 penghuni. (x, y) = pusat dasar objek. */
class HidingSpot(val type: SpotType, val x: Float, val y: Float) {
    var state = SpotState.INTACT
    var occupant: Survivor? = null
    var suspicion = 0f          // 0..1, dipakai JasonDecisionEngine
    var recheckCooldown = 0f    // > 0: baru diperiksa, Jason mengabaikan
    var hp = GameConfig.SPOT_HP // ketahanan: berkurang tiap diserang saat berpenghuni
    val damaged get() = hp < GameConfig.SPOT_HP

    val usable get() = state == SpotState.INTACT && occupant == null

    val regionName: String
        get() = "obj_${type.id}_" + when (state) {
            SpotState.INTACT -> "intact"
            SpotState.BEING_CHECKED -> "checked"
            SpotState.BROKEN_CLEAN -> "broken_clean"
            SpotState.BROKEN_BLOODY -> "broken_bloody"
        }
}
