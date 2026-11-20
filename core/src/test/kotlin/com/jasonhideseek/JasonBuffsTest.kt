package com.jasonhideseek

import com.jasonhideseek.systems.JasonBuff
import com.jasonhideseek.systems.JasonBuffs
import com.jasonhideseek.systems.MapEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class JasonBuffsTest {
    @Test fun iCanHearYouRaisesDetectionAndExpires() {
        val b = JasonBuffs(Random(1))
        assertEquals(1f, b.viewMul, 0f)
        b.grant(JasonBuff.I_CAN_HEAR_YOU)
        assertTrue(b.viewMul > 1f && b.hearChance > 0.9f && b.suspicionBonus > 0f)
        b.update(JasonBuff.I_CAN_HEAR_YOU.duration + 0.1f)
        assertFalse(b.any)
        assertEquals(1f, b.viewMul, 0f)
    }

    @Test fun breakBuffChanceIsSmall() {
        val b = JasonBuffs(Random(7))
        val hits = (0 until 5000).count { b.rollBreak() != null }
        assertTrue("hits=$hits", hits in 300..900)
    }

    @Test fun everyMapHasAnEffect() = assertEquals(5, MapEffect.values().size)
}
