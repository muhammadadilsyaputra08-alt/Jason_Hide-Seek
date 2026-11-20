package com.jasonhideseek.systems

import com.jasonhideseek.config.GameConfig
import java.util.Random

/** Buff sementara Jason. Didapat dari mengeliminasi survivor (pasti) atau menghancurkan barang (peluang kecil). */
enum class JasonBuff(val title: String, val desc: String, val duration: Float) {
    I_CAN_HEAR_YOU("I Can Hear You", "Peluang menemukan survivor naik (pandang, suara, spot)", 20f),
    BLOODLUST("Bloodlust", "Kecepatan mengejar +18%", 15f),
    HEAVY_AXE("Heavy Axe", "Jangkauan serangan lebih jauh", 18f),
    STALKER("Stalker", "Patroli & pencarian lebih cepat", 20f)
}

class JasonBuffs(private val rng: Random) {
    /** Buff aktif -> sisa detik. */
    val active = LinkedHashMap<JasonBuff, Float>()

    fun has(b: JasonBuff) = active.containsKey(b)
    val any get() = active.isNotEmpty()

    fun grant(b: JasonBuff) { active[b] = b.duration }
    fun randomBuff(): JasonBuff = JasonBuff.values()[rng.nextInt(JasonBuff.values().size)]
    /** Peluang buff saat Jason menghancurkan barang kosong. */
    fun rollBreak(): JasonBuff? = if (rng.nextFloat() < GameConfig.BUFF_BREAK_CHANCE) randomBuff() else null

    fun update(dt: Float) {
        val it = active.entries.iterator()
        while (it.hasNext()) { val e = it.next(); e.setValue(e.value - dt); if (e.value <= 0f) it.remove() }
    }

    /** Jumlah survivor yang sudah lolos: Jason makin agresif. */
    var escapes = 0
    private val aggr get() = 1f + GameConfig.AGGRO_PER_ESCAPE * escapes

    private val ich get() = has(JasonBuff.I_CAN_HEAR_YOU)
    val viewMul get() = (if (ich) 1.35f else 1f) * aggr
    val proxMul get() = if (ich) 1.7f else 1f
    val fovExtra get() = if (ich) 40f else 0f
    val detectMul get() = (if (ich) 1.4f else 1f) * aggr
    val suspicionBonus get() = if (ich) 0.25f else 0f
    val hearChance get() = Math.min(1f, GameConfig.JASON_HEAR_CHANCE + (if (ich) 0.5f else 0f) + 0.08f * escapes)
    val chaseMul get() = (if (has(JasonBuff.BLOODLUST)) 1.18f else 1f) * (1f + 0.06f * escapes)
    val patrolMul get() = (if (has(JasonBuff.STALKER)) 1.25f else 1f) * (1f + 0.10f * escapes)
    val attackBonus get() = if (has(JasonBuff.HEAVY_AXE)) 7f else 0f
}
