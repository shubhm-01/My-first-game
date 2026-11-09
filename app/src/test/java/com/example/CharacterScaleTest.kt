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
}
