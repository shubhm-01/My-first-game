package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatnaSaveStateTest {
    @Test
    fun sanitizesInvalidCoordinatesAndClampsWorldBounds() {
        val safe = PatnaSaveState(
            playerX = Float.NaN,
            playerZ = -900f,
            carX = Float.POSITIVE_INFINITY,
            carZ = 900f
        ).sanitized()

        assertEquals(-18f, safe.playerX, 0f)
        assertEquals(-450f, safe.playerZ, 0f)
        assertEquals(-9.5f, safe.carX, 0f)
        assertEquals(450f, safe.carZ, 0f)
    }

    @Test
    fun wrapsCameraAndVehicleYawIntoOneFullTurn() {
        val safe = PatnaSaveState(
            carYaw = (4f * kotlin.math.PI.toFloat()) + 0.5f,
            cameraYaw = -1f
        ).sanitized()

        assertEquals(0.5f, safe.carYaw, 0.0001f)
        assertEquals((2f * kotlin.math.PI.toFloat()) - 1f, safe.cameraYaw, 0.0001f)
    }

    @Test
    fun clampsCameraLookHeightAndKeepsProgressFlags() {
        val safe = PatnaSaveState(
            cameraLookHeight = 100f,
            hasMet = true,
            driving = true
        ).sanitized()

        assertEquals(22f, safe.cameraLookHeight, 0f)
        assertTrue(safe.hasMet)
        assertTrue(safe.driving)
    }

    @Test
    fun replacesNonFiniteCameraHeightWithSafeDefault() {
        val safe = PatnaSaveState(cameraLookHeight = Float.NEGATIVE_INFINITY).sanitized()

        assertEquals(2.8f, safe.cameraLookHeight, 0f)
    }
}
