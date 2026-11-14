package com.example

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PedestrianRouteTest {
    @Test
    fun allDailyRoutesKeepBodyClearanceFromStaticWorldGeometry() {
        val clearance = 0.45f
        assertTrue(
            "Meera's route must pass the market frontage and bench without clipping",
            pedestrianRouteAvoidsColliders(MEERA_DAILY_ROUTE, PATNA_COLLIDERS, clearance)
        )
        assertTrue(
            "Bank employee route must remain outside building and street-furniture bounds",
            pedestrianRouteAvoidsColliders(BANK_EMPLOYEE_WAYPOINTS, PATNA_COLLIDERS, clearance)
        )
        assertTrue(
            "Farmer route must go around, not through, the market stall",
            pedestrianRouteAvoidsColliders(MARKET_FARMER_WAYPOINTS, PATNA_COLLIDERS, clearance)
        )
    }

    @Test
    fun previousMeeraShortcutThroughMarketStallAndBenchIsRejected() {
        val previousRoute = listOf(
            ActorWaypoint(-14f, 34f),
            ActorWaypoint(-8f, 5f),
            ActorWaypoint(23f, -28f),
            ActorWaypoint(-20f, -43f),
            ActorWaypoint(-30f, -5f),
            ActorWaypoint(-21f, 20f)
        )
        assertFalse(pedestrianRouteAvoidsColliders(previousRoute, PATNA_COLLIDERS, 0.45f))
    }

    @Test
    fun invalidRouteAndColliderDataFailClosed() {
        assertFalse(pedestrianRouteAvoidsColliders(listOf(ActorWaypoint(0f, 0f)), PATNA_COLLIDERS, 0.45f))
        assertFalse(
            pedestrianRouteAvoidsColliders(
                listOf(ActorWaypoint(Float.NaN, 0f), ActorWaypoint(1f, 1f)),
                PATNA_COLLIDERS,
                0.45f
            )
        )
        assertFalse(pedestrianRouteAvoidsColliders(MEERA_DAILY_ROUTE, PATNA_COLLIDERS, Float.NaN))
        assertFalse(
            pedestrianRouteAvoidsColliders(
                MEERA_DAILY_ROUTE,
                listOf(WorldCollider(2f, 1f, 0f, 1f)),
                0.45f
            )
        )
    }
}
