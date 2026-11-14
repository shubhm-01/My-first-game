package com.example

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** One complete 24-hour in-game day takes 24 real minutes (one game minute per second). */
internal const val PATNA_WORLD_DAY_LENGTH_SECONDS = 24f * 60f
internal const val PATNA_WORLD_START_MINUTE = 8 * 60
private const val PATNA_MINUTES_PER_DAY = 24f * 60f

internal data class PatnaWorldLighting(
    val sunIntensityLux: Float,
    val sunColorRed: Float,
    val sunColorGreen: Float,
    val sunColorBlue: Float,
    val sunDirectionX: Float,
    val sunDirectionY: Float,
    val sunDirectionZ: Float,
    val streetLampCandela: Float
)

internal fun normalizePatnaWorldMinuteOfDay(value: Float): Float {
    if (!value.isFinite()) return PATNA_WORLD_START_MINUTE.toFloat()
    return ((value % PATNA_MINUTES_PER_DAY) + PATNA_MINUTES_PER_DAY) % PATNA_MINUTES_PER_DAY
}

/**
 * Advances the local simulation clock without consulting wall-clock time.
 * Invalid frame deltas are ignored and an invalid day length falls back to the mobile-friendly
 * 24-minute cycle. The result is always in [0, 1440).
 */
internal fun advancePatnaWorldMinutes(
    currentMinuteOfDay: Float,
    deltaSeconds: Float,
    dayLengthSeconds: Float = PATNA_WORLD_DAY_LENGTH_SECONDS
): Float {
    val current = normalizePatnaWorldMinuteOfDay(currentMinuteOfDay)
    val safeDelta = deltaSeconds.takeIf { it.isFinite() && it > 0f } ?: 0f
    val safeDayLength = dayLengthSeconds.takeIf { it.isFinite() && it > 0f }
        ?: PATNA_WORLD_DAY_LENGTH_SECONDS
    return (current + safeDelta * PATNA_MINUTES_PER_DAY / safeDayLength) % PATNA_MINUTES_PER_DAY
}

internal fun formatPatnaWorldTime(minuteOfDay: Float): String {
    val wholeMinutes = normalizePatnaWorldMinuteOfDay(minuteOfDay).toInt()
    val hour = wholeMinutes / 60
    val minute = wholeMinutes % 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

internal fun patnaDayPhase(minuteOfDay: Float): String {
    val hour = normalizePatnaWorldMinuteOfDay(minuteOfDay) / 60f
    return when {
        hour < 5.5f || hour >= 19f -> "NIGHT"
        hour < 11f -> "MORNING"
        hour < 16.5f -> "AFTERNOON"
        else -> "EVENING"
    }
}

private fun gaussian(hour: Float, center: Float, width: Float): Float {
    val distance = ((hour - center) / width).toDouble()
    return exp(-(distance * distance)).toFloat()
}

/**
 * A deliberately conservative mobile lighting curve: a low readable night fill, warm twilight,
 * one moving directional sun and shadow-free local street lamps. No per-frame material rebuilds.
 */
internal fun patnaLightingAt(minuteOfDay: Float): PatnaWorldLighting {
    val hour = normalizePatnaWorldMinuteOfDay(minuteOfDay) / 60f
    val solarAngle = ((hour - 6f) / 12f * PI).toFloat()
    val daylight = sin(solarAngle.toDouble()).toFloat().coerceAtLeast(0f)
    val daylightCurve = daylight.pow(1.35f)
    val twilight = max(gaussian(hour, 6.8f, 1.05f), gaussian(hour, 17.2f, 1.05f))

    val sunIntensity = 1_200f + 82_000f * daylightCurve
    val red = (0.66f + 0.34f * daylightCurve + 0.08f * twilight).coerceIn(0f, 1f)
    val green = (0.73f + 0.25f * daylightCurve - 0.08f * twilight).coerceIn(0f, 1f)
    val blue = (0.91f + 0.07f * daylightCurve - 0.22f * twilight).coerceIn(0f, 1f)

    // The light keeps a shallow downward direction below the horizon so the night scene remains
    // legible; its low intensity and warm local lamps provide the dusk/night distinction.
    // Filament LightManager.setDirection requires a unit vector.
    val rawDirectionX = cos(solarAngle) * 0.6f
    val rawDirectionY = -max(0.15f, daylight)
    val rawDirectionZ = -0.5f
    val directionLength = sqrt(
        rawDirectionX * rawDirectionX + rawDirectionY * rawDirectionY + rawDirectionZ * rawDirectionZ
    ).coerceAtLeast(0.0001f)
    val directionX = rawDirectionX / directionLength
    val directionY = rawDirectionY / directionLength
    val directionZ = rawDirectionZ / directionLength
    val streetLampCandela = 260f * (1f - daylightCurve).coerceIn(0f, 1f).pow(1.2f)

    return PatnaWorldLighting(
        sunIntensityLux = sunIntensity,
        sunColorRed = red,
        sunColorGreen = green,
        sunColorBlue = blue,
        sunDirectionX = directionX,
        sunDirectionY = directionY,
        sunDirectionZ = directionZ,
        streetLampCandela = streetLampCandela
    )
}
