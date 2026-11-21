package com.jasonhideseek.world

/** Tahap pintu Ruang Kedatangan Jason (sekali pakai; sisa kerusakan permanen). */
enum class ArrivalStage { LOCKED, WARNING, BREAKING, BROKEN }

/** Data `jason_arrival` satu map (schema v3). Koordinat = sel grid (origin kiri-atas). */
class ArrivalDef(
    val roomId: String, val roomName: String,
    val boundsCol: Int, val boundsRow: Int, val boundsW: Int, val boundsH: Int,
    val spawnCol: Int, val spawnRow: Int,
    val doorCol: Int, val doorRow: Int, val material: String, val doorType: String,
    val prepDelay: Float, val warnDuration: Float, val breakDuration: Float
) {
    val totalDuration get() = prepDelay + warnDuration + breakDuration
    fun inChamber(c: Int, r: Int) = c >= boundsCol && r >= boundsRow && c < boundsCol + boundsW && r < boundsRow + boundsH
}

/**
 * Linimasa pintu: LOCKED [0,prep) -> WARNING [prep,prep+warn) -> BREAKING [..,total) -> BROKEN.
 * Murni logika (tanpa libGDX) agar mudah diuji.
 */
class ArrivalSequence(val def: ArrivalDef) {
    var t = 0f
        private set
    val stage get() = stageAt(t)
    fun update(dt: Float) { t += dt }

    fun stageAt(time: Float) = when {
        time < def.prepDelay -> ArrivalStage.LOCKED
        time < def.prepDelay + def.warnDuration -> ArrivalStage.WARNING
        time < def.totalDuration -> ArrivalStage.BREAKING
        else -> ArrivalStage.BROKEN
    }

    /** 0..1 dalam tahap WARNING (intensitas getar/kedip lampu naik). */
    val warnProgress get() = ((t - def.prepDelay) / def.warnDuration).coerceIn(0f, 1f)

    /** Region sprite pintu untuk tahap saat ini. */
    fun doorRegion(): String = "door_arrival_${def.material}_" + when (stage) {
        ArrivalStage.LOCKED, ArrivalStage.WARNING -> "closed"
        ArrivalStage.BREAKING -> "cracked"
        ArrivalStage.BROKEN -> "debris"
    }

    /** Offset getar horizontal (px) saat WARNING/BREAKING. */
    fun shakeX(time: Float): Float = when (stage) {
        ArrivalStage.WARNING -> Math.sin((time * 60f).toDouble()).toFloat() * (0.6f + 1.4f * warnProgress)
        ArrivalStage.BREAKING -> Math.sin((time * 90f).toDouble()).toFloat() * 2.4f
        else -> 0f
    }

    /** Lampu ruang berkedip saat WARNING/BREAKING. */
    fun lampOn(time: Float): Boolean = when (stage) {
        ArrivalStage.LOCKED -> true
        ArrivalStage.WARNING -> ((time * (4f + 10f * warnProgress)).toInt() % 2) == 0
        ArrivalStage.BREAKING -> ((time * 18f).toInt() % 3) != 0
        ArrivalStage.BROKEN -> false
    }
}
