package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PatnaWorldClockTest {
    @Test
    fun oneRealSecondAdvancesOneInGameMinute() {
        assertEquals(481f, advancePatnaWorldMinutes(480f, 1f), 0.0001f)
    }

    @Test
    fun clockWrapsAtMidnight() {
        assertEquals(0f, advancePatnaWorldMinutes(1_439f, 1f), 0.0001f)
    }

    @Test
    fun invalidFrameDeltasDoNotMoveClockBackwards() {
        assertEquals(480f, advancePatnaWorldMinutes(480f, Float.NaN), 0.0001f)
        assertEquals(480f, advancePatnaWorldMinutes(480f, -2f), 0.0001f)
        assertEquals(481f, advancePatnaWorldMinutes(480f, 1f, Float.NaN), 0.0001f)
    }

    @Test
    fun timeLabelUses24HourClockAndWraps() {
        assertEquals("08:05", formatPatnaWorldTime(485.9f))
        assertEquals("00:07", formatPatnaWorldTime(1_447f))
        assertEquals("08:00", formatPatnaWorldTime(Float.NaN))
    }

    @Test
    fun dayPhaseChangesAcrossTheDay() {
        assertEquals("NIGHT", patnaDayPhase(2f * 60f))
        assertEquals("MORNING", patnaDayPhase(8f * 60f))
        assertEquals("AFTERNOON", patnaDayPhase(13f * 60f))
        assertEquals("EVENING", patnaDayPhase(18f * 60f))
    }

    @Test
    fun noonIsBrighterAndNightStreetLampsAreBrighterThanDay() {
        val noon = patnaLightingAt(12f * 60f)
        val night = patnaLightingAt(0f)

        assertTrue(noon.sunIntensityLux > night.sunIntensityLux)
        assertEquals(0f, noon.streetLampCandela, 0.001f)
        assertTrue(night.streetLampCandela > 200f)
    }

    @Test
    fun sunriseAndSunsetHaveWarmerLightThanMidday() {
        val dawn = patnaLightingAt(6.8f * 60f)
        val noon = patnaLightingAt(12f * 60f)
        val dusk = patnaLightingAt(17.2f * 60f)

        assertTrue(dawn.sunColorRed - dawn.sunColorBlue > noon.sunColorRed - noon.sunColorBlue)
        assertTrue(dusk.sunColorRed - dusk.sunColorBlue > noon.sunColorRed - noon.sunColorBlue)
    }

    @Test
    fun invalidTimeStillProducesFiniteLighting() {
        val lighting = patnaLightingAt(Float.NaN)
        val values = listOf(
            lighting.sunIntensityLux, lighting.sunColorRed, lighting.sunColorGreen,
            lighting.sunColorBlue, lighting.sunDirectionX, lighting.sunDirectionY,
            lighting.sunDirectionZ, lighting.streetLampCandela
        )

        assertTrue(values.all { it.isFinite() })
    }
}
