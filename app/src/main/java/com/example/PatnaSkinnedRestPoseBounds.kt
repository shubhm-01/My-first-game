package com.example

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import android.util.Base64

/**
 * Reads rest-pose bounds for skinned GLBs before SceneView 2.2.1 releases glTF source data.
 *
 * Some Mixamo/Blender exports store mesh vertices in centimetres below an armature scaled 0.01.
 * gltfio's raw POSITION accessor box then says 189 m although the joint-skinned mesh is 1.89 m.
 * This computes the glTF rest pose: sum(weight * world(joint) * inverseBind * position).
 *
 * The matrix/skin algorithm is adapted from SceneView 4.67.0 GltfRestPose (#4949, Apache-2.0),
 * narrowed to self-contained GLB files already stored by PatnaAssetCache. Unsupported data
 * (external buffers, sparse/compressed accessors or malformed input) fails closed to the existing
 * renderable/accessor fallback. Animation clips and morph-target deformation are not evaluated.
 */
internal object PatnaSkinnedRestPoseBounds {
    private const val GLB_MAGIC = 0x46546C67
    private const val JSON_CHUNK = 0x4E4F534A
    private const val BIN_CHUNK = 0x004E4942
    private const val MAX_FILE_BYTES = 40L * 1024L * 1024L
    private const val MAX_JSON_BYTES = 16 * 1024 * 1024
    private const val MAX_ACCESSOR_COMPONENTS = 4 * 1024 * 1024

    fun read(file: File): CharacterAabb? {
        if (!file.isFile || file.length() !in 20L..MAX_FILE_BYTES) return null
        return try {
            RandomAccessFile(file, "r").use { input ->
                val source = input.channel.map(FileChannel.MapMode.READ_ONLY, 0, input.length())
                    .order(ByteOrder.LITTLE_ENDIAN)
                fromGlb(source)
            }
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    internal fun fromGlb(source: ByteBuffer): CharacterAabb? {
        return try {
            val bytes = source.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            val start = bytes.position()
            if (bytes.remaining() < 20 || bytes.getInt(start) != GLB_MAGIC || bytes.getInt(start + 4) != 2) return null
            val declaredLength = bytes.getInt(start + 8).toLong() and 0xffffffffL
            if (declaredLength != bytes.remaining().toLong()) return null

            var offset = start + 12
            var jsonRange: Pair<Int, Int>? = null
            var binaryRange: Pair<Int, Int>? = null
            while (offset.toLong() + 8L <= start.toLong() + declaredLength) {
                val length = bytes.getInt(offset).toLong() and 0xffffffffL
                val type = bytes.getInt(offset + 4)
                val dataStart = offset + 8
                val dataEnd = dataStart.toLong() + length
                if (length > Int.MAX_VALUE || dataEnd > start.toLong() + declaredLength) return null
                when (type) {
                    JSON_CHUNK -> if (jsonRange == null) jsonRange = dataStart to length.toInt()
                    BIN_CHUNK -> if (binaryRange == null) binaryRange = dataStart to length.toInt()
                }
                offset = dataEnd.toInt()
            }
            val (jsonStart, jsonLength) = jsonRange ?: return null
            if (jsonLength !in 1..MAX_JSON_BYTES) return null
            val jsonBytes = ByteArray(jsonLength)
            bytes.duplicate().apply { position(jsonStart) }.get(jsonBytes)
            val json = JSONObject(String(jsonBytes, Charsets.UTF_8).trimEnd(' ', '\u0000', '\r', '\n', '\t'))
            val bin = binaryRange?.let { (at, length) ->
                bytes.duplicate().apply { position(at); limit(at + length) }.slice().order(ByteOrder.LITTLE_ENDIAN)
            }
            val minMax = restPoseBounds(json, AccessorReader(json, bin)) ?: return null
            CharacterAabb(
                minX = minMax[0], minY = minMax[1], minZ = minMax[2],
                maxX = minMax[3], maxY = minMax[4], maxZ = minMax[5]
            ).takeIf { it.height.isFinite() && it.height > 0.001f }
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
            null
        }
    }

    private class AccessorReader(private val json: JSONObject, binary: ByteBuffer?) {
        private val buffers: List<ByteBuffer?> = run {
            val declarations = json.optJSONArray("buffers") ?: JSONArray()
            List(declarations.length()) { index ->
                val declaration = declarations.optJSONObject(index) ?: return@List null
                val uri = declaration.optString("uri", "")
                val buffer = when {
                    uri.isEmpty() -> if (index == 0) binary else null
                    uri.startsWith("data:") -> decodeDataUri(uri)
                    else -> null
                } ?: return@List null
                val declared = declaration.optLong("byteLength", buffer.remaining().toLong())
                if (declared < 0 || declared > buffer.remaining()) null else buffer
            }
        }

        fun floats(index: Int): FloatArray? {
            val accessor = json.optJSONArray("accessors")?.optJSONObject(index) ?: return null
            if (accessor.has("sparse")) return null
            val view = json.optJSONArray("bufferViews")?.optJSONObject(accessor.optInt("bufferView", -1)) ?: return null
            val buffer = buffers.getOrNull(view.optInt("buffer", 0)) ?: return null
            val componentsPerElement = when (accessor.optString("type")) {
                "SCALAR" -> 1
                "VEC2" -> 2
                "VEC3" -> 3
                "VEC4" -> 4
                "MAT4" -> 16
                else -> return null
            }
            val componentType = accessor.optInt("componentType", 0)
            val componentBytes = when (componentType) {
                5120, 5121 -> 1
                5122, 5123 -> 2
                5125, 5126 -> 4
                else -> return null
            }
            val count = accessor.optLong("count", 0)
            val totalComponents = count * componentsPerElement
            if (count <= 0 || totalComponents > MAX_ACCESSOR_COMPONENTS || totalComponents > Int.MAX_VALUE) return null
            val elementBytes = componentsPerElement * componentBytes
            val stride = view.optInt("byteStride", elementBytes)
            if (stride < elementBytes) return null
            val viewOffset = view.optLong("byteOffset", 0)
            val accessorOffset = accessor.optLong("byteOffset", 0)
            val viewLength = view.optLong("byteLength", 0)
            val relativeEnd = accessorOffset + (count - 1) * stride + elementBytes
            val bufferOffset = viewOffset + accessorOffset
            if (viewOffset < 0 || accessorOffset < 0 || relativeEnd < accessorOffset ||
                relativeEnd > viewLength || bufferOffset + relativeEnd - accessorOffset > buffer.limit()
            ) return null

            val normalized = accessor.optBoolean("normalized", false)
            val source = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN)
            val output = FloatArray(totalComponents.toInt())
            for (element in 0 until count.toInt()) {
                val at = (bufferOffset + element.toLong() * stride).toInt()
                for (component in 0 until componentsPerElement) {
                    val byteAt = at + component * componentBytes
                    val value = when (componentType) {
                        5120 -> source.get(byteAt).toInt().let { if (normalized) maxOf(it / 127f, -1f) else it.toFloat() }
                        5121 -> (source.get(byteAt).toInt() and 0xff).let { if (normalized) it / 255f else it.toFloat() }
                        5122 -> source.getShort(byteAt).toInt().let { if (normalized) maxOf(it / 32767f, -1f) else it.toFloat() }
                        5123 -> (source.getShort(byteAt).toInt() and 0xffff).let { if (normalized) it / 65535f else it.toFloat() }
                        5125 -> (source.getInt(byteAt).toLong() and 0xffffffffL).let {
                            if (normalized) (it.toDouble() / 4294967295.0).toFloat() else it.toFloat()
                        }
                        5126 -> source.getFloat(byteAt)
                        else -> return null
                    }
                    if (!value.isFinite()) return null
                    output[element * componentsPerElement + component] = value
                }
            }
            return output
        }

        fun extent(index: Int): DoubleArray? {
            val accessor = json.optJSONArray("accessors")?.optJSONObject(index) ?: return null
            val min = accessor.optJSONArray("min")
            val max = accessor.optJSONArray("max")
            if (min != null && max != null && min.length() >= 3 && max.length() >= 3) {
                return DoubleArray(6) { if (it < 3) min.optDouble(it) else max.optDouble(it - 3) }
                    .takeIf { values -> values.all { it.isFinite() } }
            }
            val vertices = floats(index) ?: return null
            val bounds = Bounds()
            for (vertex in 0 until vertices.size / 3) {
                bounds.add(
                    vertices[vertex * 3].toDouble(),
                    vertices[vertex * 3 + 1].toDouble(),
                    vertices[vertex * 3 + 2].toDouble()
                )
            }
            return bounds.result()?.let { values -> DoubleArray(6) { values[it].toDouble() } }
        }

        private fun decodeDataUri(uri: String): ByteBuffer? {
            val comma = uri.indexOf(',')
            if (comma < 0 || !uri.substring(0, comma).endsWith(";base64", ignoreCase = true)) return null
            val decoded = try { Base64.decode(uri.substring(comma + 1), Base64.DEFAULT) }
            catch (_: IllegalArgumentException) { return null }
            return ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN)
        }
    }

    private fun restPoseBounds(json: JSONObject, accessors: AccessorReader): FloatArray? {
        val nodes = json.optJSONArray("nodes") ?: return null
        val meshes = json.optJSONArray("meshes") ?: return null
        val world = WorldTransforms(nodes)
        val skins = json.optJSONArray("skins")
        val bounds = Bounds()
        var hasSkinnedMesh = false
        val defaultSceneIndex = json.optInt("scene", -1)
        for (nodeIndex in world.instantiated(json.optJSONArray("scenes"), defaultSceneIndex)) {
            val node = nodes.optJSONObject(nodeIndex) ?: continue
            val mesh = meshes.optJSONObject(node.optInt("mesh", -1)) ?: continue
            val skin = node.optInt("skin", -1).takeIf { it >= 0 }?.let { skins?.optJSONObject(it) ?: return null }
            val joints = skin?.let { jointMatrices(it, world, accessors) ?: return null }.orEmpty()
            if (joints.isNotEmpty()) hasSkinnedMesh = true
            val primitives = mesh.optJSONArray("primitives") ?: continue
            for (primitiveIndex in 0 until primitives.length()) {
                val attributes = primitives.optJSONObject(primitiveIndex)?.optJSONObject("attributes") ?: continue
                val position = attributes.optInt("POSITION", -1).takeIf { it >= 0 } ?: continue
                val included = if (joints.isEmpty()) {
                    val box = accessors.extent(position) ?: return null
                    bounds.addBox(world[nodeIndex], box)
                    true
                } else {
                    addSkinned(attributes, position, joints, accessors, bounds)
                }
                if (!included) return null
            }
        }
        return if (hasSkinnedMesh) bounds.result() else null
    }

    private fun jointMatrices(skin: JSONObject, world: WorldTransforms, accessors: AccessorReader): List<DoubleArray>? {
        val joints = skin.optJSONArray("joints") ?: return emptyList()
        val inverseBindIndex = skin.optInt("inverseBindMatrices", -1)
        val inverseBinds = inverseBindIndex.takeIf { it >= 0 }?.let { accessors.floats(it) ?: return null }
        if (inverseBinds != null && inverseBinds.size < joints.length() * 16) return null
        return List(joints.length()) { index ->
            val joint = joints.optInt(index, -1).takeIf { it in 0 until world.count } ?: return null
            val inverseBind = inverseBinds?.let { values ->
                DoubleArray(16) { values[index * 16 + it].toDouble() }
            } ?: IDENTITY
            multiply(world[joint], inverseBind)
        }
    }

    private fun addSkinned(
        attributes: JSONObject,
        positionIndex: Int,
        jointMatrices: List<DoubleArray>,
        accessors: AccessorReader,
        bounds: Bounds
    ): Boolean {
        val sets = generateSequence(0) { it + 1 }
            .takeWhile { attributes.has("JOINTS_${it}") }
            .map { index -> attributes.optInt("JOINTS_${index}", -1) to attributes.optInt("WEIGHTS_${index}", -1) }
            .toList()
        val vertices = accessors.floats(positionIndex)
        val influences = sets.map { (jointIndex, weightIndex) ->
            val jointValues = accessors.floats(jointIndex) ?: return@map null
            val weightValues = accessors.floats(weightIndex) ?: return@map null
            jointValues to weightValues
        }
        val count = vertices?.size?.div(3) ?: 0
        val readable = vertices != null && sets.isNotEmpty() &&
            influences.all { it != null && it.first.size >= count * 4 && it.second.size >= count * 4 }
        if (!readable) {
            val box = accessors.extent(positionIndex) ?: return false
            jointMatrices.forEach { bounds.addBox(it, box) }
            return true
        }
        val point = DoubleArray(3)
        for (vertex in 0 until count) {
            point.fill(0.0)
            var totalWeight = 0.0
            for (influence in influences) {
                val (jointValues, weightValues) = influence!!
                for (slot in 0 until 4) {
                    val weight = weightValues[vertex * 4 + slot].toDouble()
                    val joint = jointValues[vertex * 4 + slot].toInt()
                    val matrix = jointMatrices.getOrNull(joint)
                    if (weight <= 0.0 || matrix == null) continue
                    totalWeight += weight
                    for (axis in 0..2) {
                        point[axis] += weight * transform(matrix, vertices!!, vertex * 3, axis)
                    }
                }
            }
            if (totalWeight > 0.0) bounds.add(point[0] / totalWeight, point[1] / totalWeight, point[2] / totalWeight)
        }
        return true
    }

    private class WorldTransforms(private val nodes: JSONArray) {
        val count = nodes.length()
        private val parents = IntArray(count) { -1 }.also { parent ->
            for (node in 0 until count) {
                val children = nodes.optJSONObject(node)?.optJSONArray("children") ?: continue
                for (index in 0 until children.length()) {
                    val child = children.optInt(index, -1)
                    if (child in 0 until count && child != node) parent[child] = node
                }
            }
        }
        private val cache = arrayOfNulls<DoubleArray>(count)

        operator fun get(node: Int): DoubleArray {
            cache[node]?.let { return it }
            val chain = ArrayList<Int>()
            var current = node
            while (current >= 0 && cache[current] == null && chain.size <= count) {
                chain += current
                current = parents[current]
            }
            check(chain.size <= count) { "glTF node hierarchy contains a cycle" }
            var matrix = if (current >= 0) cache[current]!! else IDENTITY
            for (index in chain.asReversed()) {
                matrix = multiply(matrix, local(nodes.optJSONObject(index)))
                cache[index] = matrix
            }
            return cache[node]!!
        }

        /**
         * Mirrors Filament's default-scene-only import behavior. Unioning every glTF scene can
         * include editor previews or alternate setups that are not rendered, inflating stature.
         * With multiple scenes and no declared default, fail closed so the live Filament
         * renderable hierarchy can provide the fallback measurement instead of guessing.
         */
        fun instantiated(scenes: JSONArray?, defaultSceneIndex: Int): Set<Int> {
            val roots = if (scenes != null && scenes.length() > 0) {
                val selectedScene = when {
                    defaultSceneIndex in 0 until scenes.length() -> defaultSceneIndex
                    scenes.length() == 1 -> 0
                    else -> return emptySet()
                }
                val sceneNodes = scenes.optJSONObject(selectedScene)?.optJSONArray("nodes")
                    ?: return emptySet()
                buildList {
                    for (index in 0 until sceneNodes.length()) {
                        add(sceneNodes.optInt(index, -1))
                    }
                }
            } else {
                (0 until count).filter { parents[it] < 0 }
            }
            val visited = LinkedHashSet<Int>()
            val stack = ArrayDeque(roots.filter { it in 0 until count })
            while (stack.isNotEmpty()) {
                val node = stack.removeLast()
                if (!visited.add(node)) continue
                val children = nodes.optJSONObject(node)?.optJSONArray("children") ?: continue
                for (index in 0 until children.length()) {
                    children.optInt(index, -1).takeIf { it in 0 until count }?.let(stack::addLast)
                }
            }
            return visited
        }
    }

    private class Bounds {
        private val values = doubleArrayOf(
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY
        )
        fun add(x: Double, y: Double, z: Double) {
            if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return
            values[0] = minOf(values[0], x); values[1] = minOf(values[1], y); values[2] = minOf(values[2], z)
            values[3] = maxOf(values[3], x); values[4] = maxOf(values[4], y); values[5] = maxOf(values[5], z)
        }
        fun addBox(matrix: DoubleArray, box: DoubleArray) {
            for (corner in 0 until 8) {
                val x = if (corner and 1 == 0) box[0] else box[3]
                val y = if (corner and 2 == 0) box[1] else box[4]
                val z = if (corner and 4 == 0) box[2] else box[5]
                add(
                    matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12],
                    matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13],
                    matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14]
                )
            }
        }
        fun result(): FloatArray? =
            if (values[0] <= values[3] && values.all { it.isFinite() }) FloatArray(6) { values[it].toFloat() } else null
    }

    private val IDENTITY = DoubleArray(16) { if (it % 5 == 0) 1.0 else 0.0 }

    private fun local(node: JSONObject?): DoubleArray {
        node ?: return IDENTITY
        node.optJSONArray("matrix")?.takeIf { it.length() == 16 }?.let { matrix ->
            return DoubleArray(16) { matrix.optDouble(it, 0.0) }
        }
        val t = node.optJSONArray("translation")
        val r = node.optJSONArray("rotation")
        val s = node.optJSONArray("scale")
        val tx = t?.optDouble(0, 0.0) ?: 0.0
        val ty = t?.optDouble(1, 0.0) ?: 0.0
        val tz = t?.optDouble(2, 0.0) ?: 0.0
        val sx = s?.optDouble(0, 1.0) ?: 1.0
        val sy = s?.optDouble(1, 1.0) ?: 1.0
        val sz = s?.optDouble(2, 1.0) ?: 1.0
        val x = r?.optDouble(0, 0.0) ?: 0.0
        val y = r?.optDouble(1, 0.0) ?: 0.0
        val z = r?.optDouble(2, 0.0) ?: 0.0
        val w = r?.optDouble(3, 1.0) ?: 1.0
        return doubleArrayOf(
            (1 - 2 * (y * y + z * z)) * sx, 2 * (x * y + z * w) * sx, 2 * (x * z - y * w) * sx, 0.0,
            2 * (x * y - z * w) * sy, (1 - 2 * (x * x + z * z)) * sy, 2 * (y * z + x * w) * sy, 0.0,
            2 * (x * z + y * w) * sz, 2 * (y * z - x * w) * sz, (1 - 2 * (x * x + y * y)) * sz, 0.0,
            tx, ty, tz, 1.0
        )
    }

    private fun multiply(a: DoubleArray, b: DoubleArray) = DoubleArray(16) { index ->
        val column = index / 4
        val row = index % 4
        (0 until 4).sumOf { k -> a[k * 4 + row] * b[column * 4 + k] }
    }

    private fun transform(matrix: DoubleArray, points: FloatArray, offset: Int, axis: Int) =
        matrix[axis] * points[offset] + matrix[4 + axis] * points[offset + 1] +
            matrix[8 + axis] * points[offset + 2] + matrix[12 + axis]
}
