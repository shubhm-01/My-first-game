package com.example

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import java.io.IOException
import kotlin.math.PI

private val Context.patna2047DataStore by preferencesDataStore(name = "patna_2047_save")

private object PatnaSaveKeys {
    val PLAYER_X = floatPreferencesKey("player_x")
    val PLAYER_Z = floatPreferencesKey("player_z")
    val CAR_X = floatPreferencesKey("car_x")
    val CAR_Z = floatPreferencesKey("car_z")
    val CAR_YAW = floatPreferencesKey("car_yaw")
    val CAMERA_YAW = floatPreferencesKey("camera_yaw")
    val CAMERA_LOOK_HEIGHT = floatPreferencesKey("camera_look_height")
    val HAS_MET = booleanPreferencesKey("has_met")
    val DRIVING = booleanPreferencesKey("driving")
}

/** Small, device-local save for the current playable Patna 2047 slice. */
internal data class PatnaSaveState(
    val playerX: Float = -18f,
    val playerZ: Float = 18f,
    val carX: Float = -9.5f,
    val carZ: Float = 18f,
    val carYaw: Float = 0f,
    val cameraYaw: Float = 0f,
    val cameraLookHeight: Float = 2.8f,
    val hasMet: Boolean = false,
    val driving: Boolean = false
)

/**
 * Protects the game from corrupted/out-of-range local values before applying them to
 * SceneView transforms. Coordinates remain inside the current playable world boundary.
 */
internal fun PatnaSaveState.sanitized(): PatnaSaveState {
    fun coordinate(value: Float, fallback: Float): Float =
        (if (value.isFinite()) value else fallback).coerceIn(-450f, 450f)

    fun angle(value: Float): Float {
        if (!value.isFinite()) return 0f
        val fullTurn = 2f * PI.toFloat()
        return ((value % fullTurn) + fullTurn) % fullTurn
    }

    val safeLookHeight = if (cameraLookHeight.isFinite()) cameraLookHeight else 2.8f
    return copy(
        playerX = coordinate(playerX, -18f),
        playerZ = coordinate(playerZ, 18f),
        carX = coordinate(carX, -9.5f),
        carZ = coordinate(carZ, 18f),
        carYaw = angle(carYaw),
        cameraYaw = angle(cameraYaw),
        cameraLookHeight = safeLookHeight.coerceIn(0.6f, 22f)
    )
}

internal suspend fun Context.loadPatna2047Save(): PatnaSaveState? = try {
    val values = patna2047DataStore.data.first()
    PatnaSaveState(
        playerX = values[PatnaSaveKeys.PLAYER_X] ?: -18f,
        playerZ = values[PatnaSaveKeys.PLAYER_Z] ?: 18f,
        carX = values[PatnaSaveKeys.CAR_X] ?: -9.5f,
        carZ = values[PatnaSaveKeys.CAR_Z] ?: 18f,
        carYaw = values[PatnaSaveKeys.CAR_YAW] ?: 0f,
        cameraYaw = values[PatnaSaveKeys.CAMERA_YAW] ?: 0f,
        cameraLookHeight = values[PatnaSaveKeys.CAMERA_LOOK_HEIGHT] ?: 2.8f,
        hasMet = values[PatnaSaveKeys.HAS_MET] ?: false,
        driving = values[PatnaSaveKeys.DRIVING] ?: false
    ).sanitized()
} catch (_: IOException) {
    null
}

internal suspend fun Context.savePatna2047Save(state: PatnaSaveState): Boolean = try {
    val safe = state.sanitized()
    patna2047DataStore.edit { values ->
        values[PatnaSaveKeys.PLAYER_X] = safe.playerX
        values[PatnaSaveKeys.PLAYER_Z] = safe.playerZ
        values[PatnaSaveKeys.CAR_X] = safe.carX
        values[PatnaSaveKeys.CAR_Z] = safe.carZ
        values[PatnaSaveKeys.CAR_YAW] = safe.carYaw
        values[PatnaSaveKeys.CAMERA_YAW] = safe.cameraYaw
        values[PatnaSaveKeys.CAMERA_LOOK_HEIGHT] = safe.cameraLookHeight
        values[PatnaSaveKeys.HAS_MET] = safe.hasMet
        values[PatnaSaveKeys.DRIVING] = safe.driving
    }
    true
} catch (_: IOException) {
    false
}
