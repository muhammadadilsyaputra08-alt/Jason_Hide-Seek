package com.jasonhideseek.systems

import com.jasonhideseek.entities.SurvivorStatus

enum class MatchResult { PLAYER_ESCAPED, SURVIVORS_ESCAPED, JASON_WINS, TIMES_UP }

/** Rekap akhir pertandingan untuk Result Screen. */
class MatchSummary(
    val result: MatchResult,
    val mapName: String,
    val playerStatus: SurvivorStatus,
    val escaped: Int,
    val eliminated: Int,
    val total: Int,
    val durationSec: Int,
    val keysCollected: Int,
    val escaper: String? = null,
    val jasonMode: Boolean = false,
    val jasonVictory: Boolean = false
)

/** Aturan hasil pertandingan (murni logika, mudah diuji). */
object MatchRules {
    /**
     * @param botsEscaped jumlah bot yang escaped
     * @param aliveTotal jumlah survivor (pemain + bot) yang masih Alive di map
     * @param timeUp Escape Timer habis
     * @return hasil jika pertandingan harus berakhir, null jika berlanjut
     */
    fun evaluate(player: SurvivorStatus, botsEscaped: Int, aliveTotal: Int, timeUp: Boolean): MatchResult? {
        // Pemain kabur TIDAK mengakhiri ronde: bot masih bisa menyusul sampai semua selesai (kabur/tereliminasi) atau waktu habis.
        if (aliveTotal > 0 && !timeUp) return null
        return when {
            player == SurvivorStatus.ESCAPED -> MatchResult.PLAYER_ESCAPED
            botsEscaped > 0 -> MatchResult.SURVIVORS_ESCAPED
            aliveTotal == 0 -> MatchResult.JASON_WINS
            else -> MatchResult.TIMES_UP
        }
    }
}
