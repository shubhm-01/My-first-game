package com.example

import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import io.github.sceneview.math.Transform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CharacterBoundsTest {
    @Test
    fun transformsAllEightRenderableBoundsCornersBeforeMeasuringHeight() {
        val bounds = transformedCharacterAabb(
            center = Position(0f, 1f, 0f),
            halfExtent = Size(0.5f, 1f, 0.25f),
            localTransform = Transform(
                position = Position(0f, 0.5f, 0f),
                rotation = Rotation(y = 90f),
                scale = Scale(2f, 1.5f, 1f)
            )
        )

        assertNotNull(bounds)
        assertEquals(3.0f, bounds!!.height, 0.0001f)
        assertEquals(0.5f, bounds.minY, 0.0001f)
        assertEquals(3.5f, bounds.maxY, 0.0001f)
        assertEquals(-0.25f, bounds.minX, 0.0001f)
        assertEquals(0.25f, bounds.maxX, 0.0001f)
        assertEquals(-1f, bounds.minZ, 0.0001f)
        assertEquals(1f, bounds.maxZ, 0.0001f)
    }

    @Test
    fun unionsSeparateMeshesAfterTheirIndependentNodeOffsets() {
        val lower = transformedCharacterAabb(
            center = Position(0f, 0.5f, 0f),
            halfExtent = Size(0.2f, 0.5f, 0.2f),
            localTransform = Transform(position = Position(0f, 0f, 0f))
        )!!
        val upper = transformedCharacterAabb(
            center = Position(0f, 0.4f, 0f),
            halfExtent = Size(0.2f, 0.4f, 0.2f),
            localTransform = Transform(position = Position(0f, 1.1f, 0f))
        )!!

        val merged = unionCharacterAabbs(listOf(lower, upper))

        assertNotNull(merged)
        assertEquals(1.9f, merged!!.height, 0.0001f)
        assertEquals(0f, merged.minY, 0.0001f)
        assertEquals(1.9f, merged.maxY, 0.0001f)
    }

    @Test
    fun rejectsNonFiniteOrNegativeRenderableBounds() {
        assertNull(
            transformedCharacterAabb(
                center = Position(0f, Float.NaN, 0f),
                halfExtent = Size(0.5f, 1f, 0.5f),
                localTransform = Transform()
            )
        )
        assertNull(
            transformedCharacterAabb(
                center = Position(0f, 0f, 0f),
                halfExtent = Size(-0.5f, 1f, 0.5f),
                localTransform = Transform()
            )
        )
        assertNull(unionCharacterAabbs(emptyList()))
    }
}
