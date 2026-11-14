package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
class PatnaSkinnedRestPoseBoundsTest {
    @Test
    fun centimetreMeshUnderZeroPointZeroOneArmatureMeasuresItsDrawnMetres() {
        val bounds = PatnaSkinnedRestPoseBounds.fromGlb(riggedCharacterGlb())

        assertNotNull(bounds)
        assertEquals(1.89f, bounds!!.height, 0.0001f)
        assertEquals(-0.1f, bounds.minX, 0.0001f)
        assertEquals(0.1f, bounds.maxX, 0.0001f)
        assertEquals(0f, bounds.minY, 0.0001f)
        assertEquals(1.89f, bounds.maxY, 0.0001f)
    }

    @Test
    fun fallsBackToJointMovedAccessorBoxWhenInfluenceDataCannotBeRead() {
        val bounds = PatnaSkinnedRestPoseBounds.fromGlb(riggedCharacterGlb(readableInfluences = false))

        assertNotNull(bounds)
        assertEquals(1.89f, bounds!!.height, 0.0001f)
        assertEquals(0f, bounds.minY, 0.0001f)
        assertEquals(1.89f, bounds.maxY, 0.0001f)
    }

    @Test
    fun measuresOnlyTheDeclaredDefaultSceneAndIgnoresAlternateSceneGeometry() {
        val bounds = PatnaSkinnedRestPoseBounds.fromGlb(
            riggedCharacterGlb(multipleScenes = true, defaultSceneIndex = 1)
        )

        assertNotNull(bounds)
        assertEquals(1.89f, bounds!!.height, 0.0001f)
        assertEquals(0f, bounds.minY, 0.0001f)
        assertEquals(1.89f, bounds.maxY, 0.0001f)
    }

    @Test
    fun refusesToGuessWhenMultipleScenesHaveNoDeclaredDefault() {
        assertNull(
            PatnaSkinnedRestPoseBounds.fromGlb(
                riggedCharacterGlb(multipleScenes = true, defaultSceneIndex = null)
            )
        )
    }

    @Test
    fun rejectsAnInvalidGlbInsteadOfReturningUntrustedBounds() {
        assertNull(PatnaSkinnedRestPoseBounds.fromGlb(ByteBuffer.wrap("not a GLB".toByteArray())))
    }

    private fun riggedCharacterGlb(
        readableInfluences: Boolean = true,
        multipleScenes: Boolean = false,
        defaultSceneIndex: Int? = 0
    ): ByteBuffer {
        val positions = floatArrayOf(-10f, 0f, 0f, 10f, 0f, 0f, 0f, 189f, 0f)
        val joints = ByteArray(12)
        val weights = FloatArray(12) { if (it % 4 == 0) 1f else 0f }
        val inverseBind = FloatArray(16) { if (it % 5 == 0) 1f else 0f }.also { it[13] = -100f }
        val binary = ByteBuffer.allocate(160).order(ByteOrder.LITTLE_ENDIAN).apply {
            positions.forEach(::putFloat)
            put(joints)
            weights.forEach(::putFloat)
            inverseBind.forEach(::putFloat)
        }.array()
        val influences = if (readableInfluences) {
            """"JOINTS_0":1,"WEIGHTS_0":2"""
        } else {
            """"JOINTS_0":99,"WEIGHTS_0":99"""
        }
        val sceneMetadata = buildString {
            if (defaultSceneIndex != null) append("\"scene\":$defaultSceneIndex,")
            if (multipleScenes) {
                append("\"scenes\":[{\"nodes\":[0,1,3]},{\"nodes\":[0,1]}],")
            } else {
                append("\"scenes\":[{\"nodes\":[0,1]}],")
            }
        }
        val alternateSceneNode = if (multipleScenes) {
            ",{\"mesh\":0,\"translation\":[0,500,0]}"
        } else {
            ""
        }
        val json = """
            {
              "asset":{"version":"2.0"},
              $sceneMetadata
              "nodes":[
                {"mesh":0,"skin":0},
                {"scale":[0.01,0.01,0.01],"children":[2]},
                {"translation":[0,100,0]}
                $alternateSceneNode
              ],
              "meshes":[{"primitives":[{"attributes":{"POSITION":0,$influences}}]}],
              "skins":[{"joints":[2],"inverseBindMatrices":3}],
              "accessors":[
                {"bufferView":0,"componentType":5126,"count":3,"type":"VEC3","min":[-10,0,0],"max":[10,189,0]},
                {"bufferView":1,"componentType":5121,"count":3,"type":"VEC4"},
                {"bufferView":2,"componentType":5126,"count":3,"type":"VEC4"},
                {"bufferView":3,"componentType":5126,"count":1,"type":"MAT4"}
              ],
              "bufferViews":[
                {"buffer":0,"byteOffset":0,"byteLength":36},
                {"buffer":0,"byteOffset":36,"byteLength":12},
                {"buffer":0,"byteOffset":48,"byteLength":48},
                {"buffer":0,"byteOffset":96,"byteLength":64}
              ],
              "buffers":[{"byteLength":160}]
            }
        """.trimIndent()
        val jsonBytes = json.toByteArray().let { raw ->
            raw + ByteArray((4 - raw.size % 4) % 4) { ' '.code.toByte() }
        }
        val totalBytes = 12 + 8 + jsonBytes.size + 8 + binary.size
        return ByteBuffer.allocate(totalBytes).order(ByteOrder.LITTLE_ENDIAN).apply {
            putInt(0x46546C67)
            putInt(2)
            putInt(totalBytes)
            putInt(jsonBytes.size)
            putInt(0x4E4F534A)
            put(jsonBytes)
            putInt(binary.size)
            putInt(0x004E4942)
            put(binary)
            flip()
        }
    }
}
