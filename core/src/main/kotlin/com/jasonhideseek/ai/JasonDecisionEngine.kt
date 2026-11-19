package com.jasonhideseek.ai

import com.jasonhideseek.config.GameConfig
import java.util.Random

enum class Decision { ATTACK, CHECK, SKIP }

/**
 * Fase keputusan acak Jason: Serang / Cek / Lewati.
 * Bobot dasar dimodifikasi oleh kecurigaan (suspicion) dan progres waktu ronde (panic),
 * lalu dinormalisasi sehingga selalu positif dan berjumlah 1.
 */
class JasonDecisionEngine(private val rng: Random = Random()) {

    /** @param suspicion 0..1  @param progress 0..1 (fraksi waktu perburuan yang telah berlalu) */
    fun weights(suspicion: Float, progress: Float, out: FloatArray = FloatArray(3)): FloatArray {
        val s = suspicion.coerceIn(0f, 1f)
        val p = progress.coerceIn(0f, 1f)
        val panic = p * p
        var attack = GameConfig.W_ATTACK + 0.55f * s + 0.25f * panic
        var check = GameConfig.W_CHECK + 0.40f * s + 0.10f * panic
        var skip = GameConfig.W_SKIP - 0.70f * s - 0.35f * panic
        attack = Math.max(attack, MIN_W)
        check = Math.max(check, MIN_W)
        skip = Math.max(skip, MIN_W)
        val sum = attack + check + skip
        out[0] = attack / sum
        out[1] = check / sum
        out[2] = skip / sum
        return out
    }

    fun decide(suspicion: Float, progress: Float): Decision {
        val w = weights(suspicion, progress)
        val r = rng.nextFloat()
        return when {
            r < w[0] -> Decision.ATTACK
            r < w[0] + w[1] -> Decision.CHECK
            else -> Decision.SKIP
        }
    }

    companion object { const val MIN_W = 0.02f }
}
