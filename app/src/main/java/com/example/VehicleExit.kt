package com.example

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

internal data class VehicleExitSpot(val x: Float, val z: Float)

internal const val PATNA_VEHICLE_EXIT_MAX_SPEED_MPS = 0.8f

/** A vehicle door interaction is allowed only when the vehicle is effectively stopped. */
internal fun canExitVehicleAtSpeed(
    speedMetersPerSecond: Float,
    maximumSpeedMetersPerSecond: Float = PATNA_VEHICLE_EXIT_MAX_SPEED_MPS
): Boolean =
    speedMetersPerSecond.isFinite() &&
        maximumSpeedMetersPerSecond.isFinite() &&
        maximumSpeedMetersPerSecond >= 0f &&
        abs(speedMetersPerSecond) <= maximumSpeedMetersPerSecond

/**
 * Finds a collision-free step-out point around the SUV.
 *
 * The SUV's forward axis is -Z. The passenger-side candidate is tried first, then the
 * driver-side candidate. Static AABBs are expanded by the exiting actor's radius, while
 * pedestrians/workers and moving traffic use circular separation checks. Invalid geometry
 * fails closed so the caller never places the player at an unverified fallback point.
 */
internal fun findSafeVehicleExitSpot(
    carX: Float,
    carZ: Float,
    carYawRadians: Float,
    colliders: List<WorldCollider>,
    blockers: List<PedestrianBlocker>,
    actorRadius: Float = 0.55f,
    sideOffset: Float = 3.4f,
    vehicleClearance: Float = 2.8f
): VehicleExitSpot? {
    if (!carX.isFinite() || !carZ.isFinite() || !carYawRadians.isFinite() ||
        !actorRadius.isFinite() || actorRadius <= 0f ||
        !sideOffset.isFinite() || sideOffset <= 0f ||
        !vehicleClearance.isFinite() || vehicleClearance < 0f
    ) return null

    if (colliders.any {
            !it.minX.isFinite() || !it.maxX.isFinite() ||
                !it.minZ.isFinite() || !it.maxZ.isFinite() ||
                it.minX > it.maxX || it.minZ > it.maxZ
        }
    ) return null

    if (blockers.any {
            !it.x.isFinite() || !it.z.isFinite() ||
                !it.radius.isFinite() || it.radius < 0f
        }
    ) return null

    // At yaw 0 the model faces -Z and its right/passenger side is +X.
    val rightX = cos(carYawRadians)
    val rightZ = -sin(carYawRadians)

    for (side in floatArrayOf(1f, -1f)) {
        val candidateX = carX + rightX * sideOffset * side
        val candidateZ = carZ + rightZ * sideOffset * side
        if (!candidateX.isFinite() || !candidateZ.isFinite() ||
            candidateX - actorRadius < -480f || candidateX + actorRadius > 480f ||
            candidateZ - actorRadius < -480f || candidateZ + actorRadius > 480f
        ) continue

        val fromVehicleX = candidateX - carX
        val fromVehicleZ = candidateZ - carZ
        if (fromVehicleX * fromVehicleX + fromVehicleZ * fromVehicleZ <
            vehicleClearance * vehicleClearance
        ) continue

        val hitsStaticGeometry = colliders.any { obstacle ->
            candidateX + actorRadius > obstacle.minX &&
                candidateX - actorRadius < obstacle.maxX &&
                candidateZ + actorRadius > obstacle.minZ &&
                candidateZ - actorRadius < obstacle.maxZ
        }
        if (hitsStaticGeometry) continue

        val hitsMovingActor = blockers.any { blocker ->
            val dx = candidateX - blocker.x
            val dz = candidateZ - blocker.z
            val minimumSeparation = actorRadius + blocker.radius + 0.35f
            dx * dx + dz * dz < minimumSeparation * minimumSeparation
        }
        if (!hitsMovingActor) return VehicleExitSpot(candidateX, candidateZ)
    }
    return null
}
