package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PatnaAssetCacheTest {
    private fun validPrefix(
        totalLength: Int = 24,
        version: Int = 2,
        magic: Int = 0x46546C67,
        chunkType: Int = 0x4E4F534A
    ): ByteArray = ByteBuffer.allocate(20)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(magic)
        .putInt(version)
        .putInt(totalLength)
        .putInt(totalLength - 20)
        .putInt(chunkType)
        .array()

    @Test
    fun acceptsCompleteGlbV2HeaderAndJsonChunk() {
        assertTrue(PatnaAssetCache.isValidGlbHeader(validPrefix(), 24L))
    }

    @Test
    fun rejectsHtmlOrOtherNonGlbPayloads() {
        val html = "<html>not a model</html>".toByteArray()
        assertFalse(PatnaAssetCache.isValidGlbHeader(html, html.size.toLong()))
    }

    @Test
    fun rejectsWrongVersionMagicChunkTypeAndDeclaredLength() {
        assertFalse(PatnaAssetCache.isValidGlbHeader(validPrefix(version = 1), 24L))
        assertFalse(PatnaAssetCache.isValidGlbHeader(validPrefix(magic = 0), 24L))
        assertFalse(PatnaAssetCache.isValidGlbHeader(validPrefix(chunkType = 0), 24L))
        assertFalse(PatnaAssetCache.isValidGlbHeader(validPrefix(totalLength = 28), 24L))
    }

    @Test
    fun rejectsTruncatedAndOverBudgetFiles() {
        assertFalse(PatnaAssetCache.isValidGlbHeader(ByteArray(19), 19L))
        assertFalse(PatnaAssetCache.isValidGlbHeader(validPrefix(), 24L, maxBytes = 23L))
    }

    @Test
    fun cacheNamesAreStableOpaqueAndUrlSpecific() {
        val first = PatnaAssetCache.cacheFileNameForUrl("https://assets.example/aarav.glb")
        val repeated = PatnaAssetCache.cacheFileNameForUrl("https://assets.example/aarav.glb")
        val second = PatnaAssetCache.cacheFileNameForUrl("https://assets.example/meera.glb")

        assertEquals(first, repeated)
        assertFalse(first == second)
        assertEquals(64, first.removeSuffix(".glb").length)
        assertTrue(first.endsWith(".glb"))
        assertFalse(first.contains("aarav"))
    }
}
