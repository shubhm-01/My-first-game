package com.example

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

/**
 * Small bounded disk cache for remote GLB assets.
 *
 * The two core on-foot characters are pinned in a separate cache folder so later launches can
 * still start on foot when the device is offline. Optional city assets use a bounded LRU folder.
 * Cache files are written to a temporary path and accepted only after their GLB v2 header and
 * first JSON chunk header have been validated.
 */
internal object PatnaAssetCache {
    const val MAX_MODEL_BYTES = 40L * 1024L * 1024L
    const val MAX_BACKGROUND_CACHE_BYTES = 72L * 1024L * 1024L

    private const val CONNECT_TIMEOUT_MS = 12_000
    private const val READ_TIMEOUT_MS = 30_000
    private const val GLB_HEADER_BYTES = 20
    private const val GLB_MAGIC = 0x46546C67
    private const val JSON_CHUNK_TYPE = 0x4E4F534A

    internal fun cacheFileNameForUrl(url: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(url.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "$hex.glb"
    }

    /** Validates the GLB v2 header and first JSON chunk without parsing model payloads. */
    internal fun isValidGlbHeader(
        prefix: ByteArray,
        actualSize: Long,
        maxBytes: Long = MAX_MODEL_BYTES
    ): Boolean {
        if (actualSize < GLB_HEADER_BYTES || actualSize > maxBytes || prefix.size < GLB_HEADER_BYTES) {
            return false
        }

        val header = ByteBuffer.wrap(prefix).order(ByteOrder.LITTLE_ENDIAN)
        val magic = header.int
        val version = header.int
        val declaredSize = header.int.toLong() and 0xffffffffL
        val jsonChunkLength = header.int.toLong() and 0xffffffffL
        val jsonChunkType = header.int

        return magic == GLB_MAGIC &&
            version == 2 &&
            declaredSize == actualSize &&
            jsonChunkType == JSON_CHUNK_TYPE &&
            jsonChunkLength > 0L &&
            jsonChunkLength <= actualSize - GLB_HEADER_BYTES
    }

    /**
     * Resolves a URL to a validated local file path. Returns null on download/validation failure;
     * the caller can then skip that optional asset instead of starting an unbounded second request.
     */
    suspend fun resolve(context: Context, url: String, keepOffline: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            val folder = if (keepOffline) "patna2047/core" else "patna2047/models"
            val directory = File(context.cacheDir, folder)
            if (!directory.exists() && !directory.mkdirs()) return@withContext null

            val fileName = cacheFileNameForUrl(url)
            val destination = File(directory, fileName)
            if (isValidGlbFile(destination)) {
                destination.setLastModified(System.currentTimeMillis())
                return@withContext destination.absolutePath
            }

            destination.delete()
            val temporary = File(directory, "$fileName.part")
            temporary.delete()
            var connection: HttpURLConnection? = null
            try {
                connection = URL(url).openConnection() as? HttpURLConnection
                    ?: throw IOException("Unsupported model URL")
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "Accept",
                    "model/gltf-binary, application/octet-stream, */*"
                )

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) throw IOException("Model server returned $responseCode")
                if (connection.contentLength > MAX_MODEL_BYTES) throw IOException("Model exceeds cache budget")

                connection.inputStream.use { input ->
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var totalBytes = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            totalBytes += count
                            if (totalBytes > MAX_MODEL_BYTES) throw IOException("Model exceeds cache budget")
                            output.write(buffer, 0, count)
                        }
                        output.fd.sync()
                    }
                }

                if (!isValidGlbFile(temporary)) throw IOException("Downloaded file is not a complete GLB v2")
                if (!temporary.renameTo(destination)) throw IOException("Unable to finalize cached model")
                destination.setLastModified(System.currentTimeMillis())

                if (!keepOffline) trimBackgroundCache(directory)
                destination.absolutePath
            } catch (_: IOException) {
                temporary.delete()
                null
            } finally {
                connection?.disconnect()
            }
        }

    private fun isValidGlbFile(file: File): Boolean {
        if (!file.isFile) return false
        val size = file.length()
        if (size < GLB_HEADER_BYTES || size > MAX_MODEL_BYTES) return false

        val prefix = ByteArray(GLB_HEADER_BYTES)
        return try {
            file.inputStream().use { input ->
                var offset = 0
                while (offset < prefix.size) {
                    val count = input.read(prefix, offset, prefix.size - offset)
                    if (count <= 0) return@use false
                    offset += count
                }
                isValidGlbHeader(prefix, size)
            }
        } catch (_: IOException) {
            false
        }
    }

    /** Evicts oldest optional models first; core character files live in a separate pinned folder. */
    private fun trimBackgroundCache(directory: File) {
        val files = directory.listFiles { file ->
            file.isFile && file.extension.equals("glb", ignoreCase = true)
        }?.sortedBy { it.lastModified() } ?: return

        var totalBytes = files.sumOf { it.length() }
        for (file in files) {
            if (totalBytes <= MAX_BACKGROUND_CACHE_BYTES) break
            val fileBytes = file.length()
            if (file.delete()) totalBytes -= fileBytes
        }
    }
}
