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

    @Test
    fun onFootAccelerationIsBoundedByTheFrameDelta() {
        val velocity = approachPlanarVelocity(0f, 0f, 1.55f, 0f, 0.08f)
        assertEquals(0.08f, velocity.x, 0.0001f)
        assertEquals(0f, velocity.z, 0.0001f)
    }

    @Test
    fun onFootBrakingApproachesZeroWithoutOvershooting() {
        val velocity = approachPlanarVelocity(1.2f, -0.4f, 0f, 0f, 0.25f)
        assertEquals(0.2f, velocity.x, 0.0001f)
        assertEquals(-0.0666667f, velocity.z, 0.0001f)
    }

    @Test
    fun directionChangesDecelerateBeforeReversing() {
        val velocity = approachPlanarVelocity(1f, 0f, -1f, 0f, 0.3f)
        assertEquals(0.7f, velocity.x, 0.0001f)
    }

    @Test
    fun invalidVelocityInputsAreSanitized() {
        val velocity = approachPlanarVelocity(Float.NaN, 1f, Float.POSITIVE_INFINITY, 0f, Float.NaN)
        assertEquals(0f, velocity.x, 0f)
        assertEquals(1f, velocity.z, 0f)
        assertTrue(velocity.x.isFinite() && velocity.z.isFinite())
    }
}
