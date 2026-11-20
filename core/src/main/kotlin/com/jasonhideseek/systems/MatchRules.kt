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
    val escaper: String? = null
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
        if (player == SurvivorStatus.ESCAPED) return MatchResult.PLAYER_ESCAPED
        // Hanya satu survivor (pemegang kunci utuh) yang bisa keluar: ronde selesai begitu ada yang kabur.
        if (botsEscaped > 0) return MatchResult.SURVIVORS_ESCAPED
        if (aliveTotal == 0) return if (botsEscaped > 0) MatchResult.SURVIVORS_ESCAPED else MatchResult.JASON_WINS
        if (timeUp) {
            return if (player == SurvivorStatus.ELIMINATED && botsEscaped > 0) MatchResult.SURVIVORS_ESCAPED
            else MatchResult.TIMES_UP
        }
        return null
    }
}
