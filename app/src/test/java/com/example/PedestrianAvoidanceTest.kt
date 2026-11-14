package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class PedestrianAvoidanceTest {
    @Test
    fun followsTheDirectRouteWhenNoActorOrColliderBlocksIt() {
        val step = pedestrianAvoidanceStep(
            currentX = 0f,
            currentZ = 0f,
            targetX = 0f,
            targetZ = 10f,
            blockers = emptyList(),
            colliders = emptyList(),
            deltaSeconds = 0.1f,
            speedMetersPerSecond = 1.25f
        )

        assertFalse(step.yielding)
        assertEquals(0f, step.x, 0.0001f)
        assertEquals(0.0625f, step.z, 0.0001f)
    }

    @Test
    fun sidestepsAPlayerInThePedestrianWalkingLane() {
        val player = PedestrianBlocker(x = 0f, z = 1.22f, radius = 0.45f)
        val step = pedestrianAvoidanceStep(
            currentX = 0f,
            currentZ = 0f,
            targetX = 0f,
            targetZ = 10f,
            blockers = listOf(player),
            colliders = emptyList(),
            deltaSeconds = 0.1f,
            speedMetersPerSecond = 1.25f
        )

        val separation = sqrt(step.x * step.x + (step.z - player.z) * (step.z - player.z))
        assertFalse(step.yielding)
        assertTrue("The chosen step should move laterally around the player", kotlin.math.abs(step.x) > 0.02f)
        assertTrue("The step must retain the configured body clearance", separation >= 1.18f - 0.0001f)
    }

    @Test
    fun yieldsWhenEveryCandidateWouldOverlapAnotherActor() {
        val blocker = PedestrianBlocker(x = 0f, z = 0f, radius = 0.45f)
        val step = pedestrianAvoidanceStep(
            currentX = 0f,
            currentZ = 0f,
            targetX = 0f,
            targetZ = 10f,
            blockers = listOf(blocker),
            colliders = emptyList(),
            deltaSeconds = 0.1f,
            speedMetersPerSecond = 1.25f
        )

        assertTrue(step.yielding)
        assertEquals(0f, step.x, 0f)
        assertEquals(0f, step.z, 0f)
    }

    @Test
    fun neverSidestepsThroughStaticWorldGeometry() {
        val wall = WorldCollider(minX = -2f, maxX = 2f, minZ = -1f, maxZ = 3f)
        val step = pedestrianAvoidanceStep(
            currentX = 0f,
            currentZ = 0f,
            targetX = 0f,
            targetZ = 10f,
            blockers = emptyList(),
            colliders = listOf(wall),
            deltaSeconds = 0.1f,
            speedMetersPerSecond = 1.25f
        )

        assertTrue(step.yielding)
        assertEquals(0f, step.x, 0f)
        assertEquals(0f, step.z, 0f)
    }

    @Test
    fun invalidMotionOrBlockerDataFailsClosed() {
        val invalid = pedestrianAvoidanceStep(
            currentX = 0f,
            currentZ = 0f,
            targetX = Float.NaN,
            targetZ = 10f,
            blockers = listOf(PedestrianBlocker(1f, 1f, Float.NaN)),
            colliders = emptyList(),
            deltaSeconds = 0.1f,
            speedMetersPerSecond = 1.25f
        )

        assertTrue(invalid.yielding)
        assertEquals(0f, invalid.x, 0f)
        assertEquals(0f, invalid.z, 0f)
    }
}
