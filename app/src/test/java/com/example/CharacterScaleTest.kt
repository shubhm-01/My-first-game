package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterScaleTest {
    @Test
    fun scalesFromVerticalBoundsToRequestedStature() {
        val nativeHeight = 2.4f
        val targetHeight = 1.72f

        val factor = uniformScaleForCharacterHeight(nativeHeight, targetHeight)

        assertEquals(targetHeight, nativeHeight * factor, 0.0001f)
    }

    @Test
    fun rejectsInvalidBoundsWithoutProducingNanOrInfinity() {
        assertEquals(1f, uniformScaleForCharacterHeight(0f, 1.72f), 0f)
        assertEquals(1f, uniformScaleForCharacterHeight(Float.NaN, 1.72f), 0f)
        assertEquals(1f, uniformScaleForCharacterHeight(1.7f, Float.POSITIVE_INFINITY), 0f)
        assertEquals(1f, uniformScaleForCharacterHeight(1.7f, -1f), 0f)
    }

    @Test
    fun preservesRelativeHeightDifferencesBetweenAdults() {
        val nativeHeight = 2.0f
        val manScale = uniformScaleForCharacterHeight(nativeHeight, 1.72f)
        val womanScale = uniformScaleForCharacterHeight(nativeHeight, 1.64f)

        assertEquals(1.72f, nativeHeight * manScale, 0.0001f)
        assertEquals(1.64f, nativeHeight * womanScale, 0.0001f)
        assertEquals(0.08f, (nativeHeight * manScale) - (nativeHeight * womanScale), 0.0001f)
    }

    @Test
    fun keepsOffCenterCharacterFeetAtGroundAnchorAcrossYawChanges() {
        val grounding = characterGroundingFromBounds(
            centerX = 0.32f,
            centerY = 1.1f,
            centerZ = -0.24f,
            halfExtentY = 0.92f,
            scaleX = 0.5f,
            scaleY = 0.5f,
            scaleZ = 0.5f
        )

        for (yawDegrees in listOf(0f, 90f, 180f, -90f, 37f)) {
            val yawRadians = yawDegrees * kotlin.math.PI.toFloat() / 180f
            val c = kotlin.math.cos(yawRadians)
            val sn = kotlin.math.sin(yawRadians)
            val root = grounding.rootPosition(12f, 0.12f, -7f, yawDegrees)

            val reconstructedBottomX = root.x + c * grounding.localBottomX + sn * grounding.localBottomZ
            val reconstructedBottomY = root.y + grounding.localBottomY
            val reconstructedBottomZ = root.z - sn * grounding.localBottomX + c * grounding.localBottomZ

            assertEquals("feet X at yaw $yawDegrees", 12f, reconstructedBottomX, 0.0001f)
            assertEquals("feet Y at yaw $yawDegrees", 0.12f, reconstructedBottomY, 0.0001f)
            assertEquals("feet Z at yaw $yawDegrees", -7f, reconstructedBottomZ, 0.0001f)
        }
    }
}
