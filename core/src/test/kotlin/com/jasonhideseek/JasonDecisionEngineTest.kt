package com.jasonhideseek

import com.jasonhideseek.ai.Decision
import com.jasonhideseek.ai.JasonDecisionEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class JasonDecisionEngineTest {
    private val engine = JasonDecisionEngine(Random(42))

    @Test
    fun weightsAlwaysPositiveAndNormalized() {
        for (s in 0..10) for (p in 0..10) {
            val w = engine.weights(s / 10f, p / 10f)
            assertEquals(1f, w[0] + w[1] + w[2], 0.0001f)
            assertTrue(w.all { it > 0f })
        }
    }

    @Test
    fun suspicionRaisesAttackAndCheckAndLowersSkip() {
        val calm = engine.weights(0f, 0f)
        val suspicious = engine.weights(1f, 0f)
        assertTrue(suspicious[0] > calm[0])
        assertTrue(suspicious[1] > calm[1])
        assertTrue(suspicious[2] < calm[2])
    }

    @Test
    fun lateRoundIsMoreAggressive() {
        val early = engine.weights(0.2f, 0f)
        val late = engine.weights(0.2f, 1f)
        assertTrue(late[0] > early[0])
        assertTrue(late[2] < early[2])
    }

    @Test
    fun sampledDistributionFollowsWeights() {
        val w = engine.weights(0.5f, 0.5f)
        val n = 20000
        var attack = 0
        repeat(n) { if (engine.decide(0.5f, 0.5f) == Decision.ATTACK) attack++ }
        assertEquals(w[0], attack.toFloat() / n, 0.02f)
    }
}
