package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class VehicleExitTest {
    @Test
    fun prefersPassengerSideWhenBothSidesAreClear() {
        val spot = findSafeVehicleExitSpot(10f, 20f, 0f, emptyList(), emptyList())

        assertNotNull(spot)
        assertEquals(13.4f, spot!!.x, 0.0001f)
        assertEquals(20f, spot.z, 0.0001f)
    }

    @Test
    fun usesDriverSideWhenPassengerSideIsBlockedByAHouse() {
        val colliders = listOf(WorldCollider(12.5f, 14.5f, 18.5f, 21.5f))

        val spot = findSafeVehicleExitSpot(10f, 20f, 0f, colliders, emptyList())

        assertNotNull(spot)
        assertEquals(6.6f, spot!!.x, 0.0001f)
        assertEquals(20f, spot.z, 0.0001f)
    }

    @Test
    fun refusesToExitWhenBothSidesAreBlocked() {
        val colliders = listOf(
            WorldCollider(12.5f, 14.5f, 18.5f, 21.5f),
            WorldCollider(5.5f, 7.5f, 18.5f, 21.5f)
        )

        assertNull(findSafeVehicleExitSpot(10f, 20f, 0f, colliders, emptyList()))
    }

    @Test
    fun avoidsNearbyPedestriansAndMovingTraffic() {
        val blockers = listOf(
            PedestrianBlocker(13.4f, 20f, 0.8f),
            PedestrianBlocker(6.6f, 20f, 2.4f)
        )

        assertNull(findSafeVehicleExitSpot(10f, 20f, 0f, emptyList(), blockers))
    }

    @Test
    fun rotatesPassengerSideWithVehicleHeading() {
        val spot = findSafeVehicleExitSpot(
            carX = 10f,
            carZ = 20f,
            carYawRadians = (PI / 2.0).toFloat(),
            colliders = emptyList(),
            blockers = emptyList()
        )

        assertNotNull(spot)
        assertEquals(10f, spot!!.x, 0.0001f)
        assertEquals(16.6f, spot.z, 0.0001f)
    }

    @Test
    fun invalidPoseOrCollisionDataFailsClosed() {
        assertNull(findSafeVehicleExitSpot(Float.NaN, 20f, 0f, emptyList(), emptyList()))
        assertNull(
            findSafeVehicleExitSpot(
                10f, 20f, 0f,
                listOf(WorldCollider(4f, 2f, 0f, 1f)),
                emptyList()
            )
        )
        assertNull(
            findSafeVehicleExitSpot(
                10f, 20f, 0f, emptyList(),
                listOf(PedestrianBlocker(Float.NaN, 20f, 0.5f))
            )
        )
    }

    @Test
    fun requiresTheVehicleToBeNearlyStopped() {
        assertTrue(canExitVehicleAtSpeed(0f))
        assertTrue(canExitVehicleAtSpeed(0.8f))
        assertTrue(canExitVehicleAtSpeed(-0.8f))
        assertFalse(canExitVehicleAtSpeed(0.81f))
        assertFalse(canExitVehicleAtSpeed(-1.2f))
        assertFalse(canExitVehicleAtSpeed(Float.NaN))
    }
}
