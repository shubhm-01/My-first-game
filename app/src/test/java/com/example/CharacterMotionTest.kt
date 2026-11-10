package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterMotionTest {
    @Test
    fun turnsAcrossZeroUsingTheShortArc() {
        assertEquals(1f, approachYawDegrees(359f, 1f, 2f), 0.0001f)
        assertEquals(359f, approachYawDegrees(1f, 359f, 2f), 0.0001f)
    }

    @Test
    fun limitsTurnRateAndSnapsWhenTargetIsWithinOneStep() {
        assertEquals(12f, approachYawDegrees(0f, 90f, 12f), 0.0001f)
        assertEquals(90f, approachYawDegrees(84f, 90f, 12f), 0.0001f)
    }

    @Test
    fun wrapsLargeHeadingsAndKeepsTheResultFinite() {
        assertEquals(45f, approachYawDegrees(765f, 45f, 180f), 0.0001f)
        assertTrue(approachYawDegrees(Float.NaN, 45f, 180f).isFinite())
        assertTrue(approachYawDegrees(0f, Float.POSITIVE_INFINITY, 180f).isFinite())
    }

    @Test
    fun invalidTurnRateDoesNotCreateAnUnboundedRotationStep() {
        assertEquals(0f, approachYawDegrees(0f, 90f, Float.POSITIVE_INFINITY), 0f)
        assertEquals(0f, approachYawDegrees(0f, 90f, Float.NaN), 0f)
    }
}
