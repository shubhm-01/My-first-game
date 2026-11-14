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

        assertEquals(PATNA_CAMERA_MAX_LOOK_HEIGHT, safe.cameraLookHeight, 0f)
        assertTrue(safe.hasMet)
        assertTrue(safe.driving)
    }

    @Test
    fun replacesNonFiniteCameraHeightWithSafeDefault() {
        val safe = PatnaSaveState(cameraLookHeight = Float.NEGATIVE_INFINITY).sanitized()

        assertEquals(PATNA_CAMERA_DEFAULT_LOOK_HEIGHT, safe.cameraLookHeight, 0f)
    }
    @Test
    fun clampsLegacyFootLevelCameraTargetsUpToTheTorso() {
        val safe = PatnaSaveState(cameraLookHeight = 0.6f).sanitized()

        assertEquals(PATNA_CAMERA_MIN_LOOK_HEIGHT, safe.cameraLookHeight, 0f)
    }

    @Test
    fun keepsSavedCameraLookTargetInsideHumanFramingRange() {
        val safe = PatnaSaveState(cameraLookHeight = 2.8f).sanitized()

        assertEquals(PATNA_CAMERA_MAX_LOOK_HEIGHT, safe.cameraLookHeight, 0f)
    }
    @Test
    fun wrapsPersistedWorldTimeIntoOneDay() {
        assertEquals(1_439f, PatnaSaveState(worldMinuteOfDay = -1f).sanitized().worldMinuteOfDay, 0.0001f)
        assertEquals(7f, PatnaSaveState(worldMinuteOfDay = 1_447f).sanitized().worldMinuteOfDay, 0.0001f)
    }

    @Test
    fun invalidPersistedWorldTimeFallsBackToMorning() {
        assertEquals(
            PATNA_WORLD_START_MINUTE.toFloat(),
            PatnaSaveState(worldMinuteOfDay = Float.NaN).sanitized().worldMinuteOfDay,
            0f
        )
        assertEquals(
            PATNA_WORLD_START_MINUTE.toFloat(),
            PatnaSaveState(worldMinuteOfDay = Float.POSITIVE_INFINITY).sanitized().worldMinuteOfDay,
            0f
        )
    }
}
