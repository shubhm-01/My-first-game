package com.example

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.sceneview.Scene
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.PlaneNode
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.CylinderNode
import io.github.sceneview.node.GeometryNode
import io.github.sceneview.geometries.Geometry
import io.github.sceneview.geometries.UvCoordinate
import io.github.sceneview.math.Direction
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.math.Scale
import io.github.sceneview.math.Size
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private const val MAN_GLB = "https://grab3d.com/dl/man-rigged/man-rigged.glb"
private const val WOMAN_GLB = "https://grab3d.com/dl/woman-rigged/woman-rigged.glb"
private const val HOUSE_GLB = "https://grab3d.com/dl/modern-house/modern-house.glb"
private const val SUV_GLB = "https://grab3d.com/dl/1990s-suv/1990s-suv.glb"
private const val TREE_GLB = "https://grab3d.com/dl/mangrove-tree/mangrove-tree.glb"
private const val STREETLIGHT_GLB = "https://grab3d.com/dl/street-light/street-light.glb"
private const val BUSSTOP_GLB = "https://grab3d.com/dl/bus-stop-shelter/bus-stop-shelter.glb"
private const val FARM_STAND_GLB = "https://grab3d.com/dl/farm-stand/farm-stand.glb"
private const val HIGHRISE_GLB = "https://www.innerscene.com/api/library/mixed-use-high-rise-27a41221/download"
private const val BANK_GLB = "https://grab3d.com/dl/bank/bank.glb"
private const val UNIVERSITY_GLB = "https://grab3d.com/dl/university-building/university-building.glb"
private const val POWER_PLANT_GLB = "https://grab3d.com/dl/power-plant/power-plant.glb"
private const val BUSINESSMAN_GLB = "https://grab3d.com/dl/businessman-rigged/businessman-rigged.glb"
private const val FARMER_GLB = "https://grab3d.com/dl/farmer-rigged/farmer-rigged.glb"

private class PatnaInput {
    var forward = false
    var back = false
    var left = false
    var right = false
    // Camera yaw is deliberately unbounded: a full orbit is allowed.
    var yaw = 0f
    var cameraHeight = 3.9f
    // Camera target height is the pitch control: swipe up to look towards the horizon/sky.
    var cameraLookHeight = 2.8f
    var interact = false
    var driving by mutableStateOf(false)
    var playerX = -18f
    var playerZ = 18f
    var carX = -9.5f
    var carZ = 18f
    var carYaw = 0f
    var carSpeed = 0f

    fun restore(saved: PatnaSaveState) {
        val safe = saved.sanitized()
        playerX = safe.playerX
        playerZ = safe.playerZ
        carX = safe.carX
        carZ = safe.carZ
        carYaw = safe.carYaw
        yaw = safe.cameraYaw
        cameraLookHeight = safe.cameraLookHeight
        driving = safe.driving
        carSpeed = 0f
    }

    fun snapshot(hasMet: Boolean): PatnaSaveState = PatnaSaveState(
        playerX = playerX,
        playerZ = playerZ,
        carX = carX,
        carZ = carZ,
        carYaw = carYaw,
        cameraYaw = yaw,
        cameraLookHeight = cameraLookHeight,
        hasMet = hasMet,
        driving = driving
    ).sanitized()
}

internal data class ActorWaypoint(val x: Float, val z: Float, val pause: Float = 0f)
private const val CHARACTER_GROUND_Y = 0.12f
private const val BACKGROUND_CITY_ASSET_COUNT = 17

/**
 * Stores the model-space bottom-center anchor after the node's own scale is applied.
 * The offset is re-rotated on every movement update so off-center GLB pivots do not
 * make a character snap vertically or drift sideways as it turns.
 */
internal data class CharacterGrounding(
    val localBottomX: Float,
    val localBottomY: Float,
    val localBottomZ: Float
) {
    fun rootPosition(anchorX: Float, groundY: Float, anchorZ: Float, yawDegrees: Float): Position {
        val safeYaw = if (yawDegrees.isFinite()) yawDegrees else 0f
        val yawRadians = safeYaw * PI.toFloat() / 180f
        val c = cos(yawRadians)
        val sn = sin(yawRadians)
        val bottomWorldX = c * localBottomX + sn * localBottomZ
        val bottomWorldZ = -sn * localBottomX + c * localBottomZ
        return Position(
            x = anchorX - bottomWorldX,
            y = groundY - localBottomY,
            z = anchorZ - bottomWorldZ
        )
    }
}

internal fun characterGroundingFromBounds(
    centerX: Float,
    centerY: Float,
    centerZ: Float,
    halfExtentY: Float,
    scaleX: Float,
    scaleY: Float,
    scaleZ: Float
): CharacterGrounding = CharacterGrounding(
    localBottomX = centerX * scaleX,
    localBottomY = (centerY - halfExtentY) * scaleY,
    localBottomZ = centerZ * scaleZ
)

private fun ModelNode.characterGrounding(): CharacterGrounding = characterGroundingFromBounds(
    centerX = center.x,
    centerY = center.y,
    centerZ = center.z,
    halfExtentY = halfExtent.y,
    scaleX = scale.x,
    scaleY = scale.y,
    scaleZ = scale.z
)

private data class PedestrianRoutine(
    val node: ModelNode,
    val waypoints: List<ActorWaypoint>,
    val walkAnimationSpeed: Float,
    val grounding: CharacterGrounding,
    var x: Float,
    var z: Float,
    var yawDegrees: Float
)

/**
 * Converts a model's measured, unscaled vertical AABB height into a uniform scale.
 * Returns 1f for invalid bounds so malformed GLBs never create Infinity/NaN transforms.
 */
internal fun uniformScaleForCharacterHeight(unscaledHeightMeters: Float, targetHeightMeters: Float): Float =
    if (unscaledHeightMeters.isFinite() && unscaledHeightMeters > 0f &&
        targetHeightMeters.isFinite() && targetHeightMeters > 0f
    ) targetHeightMeters / unscaledHeightMeters else 1f

/** Turns toward a heading along the shortest arc, with a frame-rate independent angular limit. */
internal fun approachYawDegrees(currentDegrees: Float, targetDegrees: Float, maxDegreesPerStep: Float): Float {
    if (!currentDegrees.isFinite() || !targetDegrees.isFinite()) return 0f

    fun wrap(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

    val current = wrap(currentDegrees)
    val target = wrap(targetDegrees)
    val shortestDelta = ((target - current + 540f) % 360f) - 180f
    val maxStep = if (maxDegreesPerStep.isFinite()) maxDegreesPerStep.coerceAtLeast(0f) else 0f
    return wrap(current + shortestDelta.coerceIn(-maxStep, maxStep))
}

/** Planar world-space velocity used to ease on-foot starts, stops and direction changes. */
internal data class PlanarVelocity(val x: Float, val z: Float)

/**
 * Moves a planar velocity toward its target by at most [maxDelta] metres/second per frame.
 * This keeps acceleration independent of frame rate and prevents direction changes from snapping.
 */
internal fun approachPlanarVelocity(
    currentX: Float,
    currentZ: Float,
    targetX: Float,
    targetZ: Float,
    maxDelta: Float
): PlanarVelocity {
    val safeCurrentX = currentX.takeIf { it.isFinite() } ?: 0f
    val safeCurrentZ = currentZ.takeIf { it.isFinite() } ?: 0f
    val safeTargetX = targetX.takeIf { it.isFinite() } ?: 0f
    val safeTargetZ = targetZ.takeIf { it.isFinite() } ?: 0f
    val safeMaxDelta = if (maxDelta.isFinite()) maxDelta.coerceAtLeast(0f) else 0f
    val deltaX = safeTargetX - safeCurrentX
    val deltaZ = safeTargetZ - safeCurrentZ
    val distance = sqrt(deltaX * deltaX + deltaZ * deltaZ)
    if (distance <= safeMaxDelta || distance <= 0.000001f) {
        return PlanarVelocity(safeTargetX, safeTargetZ)
    }
    val fraction = safeMaxDelta / distance
    return PlanarVelocity(
        safeCurrentX + deltaX * fraction,
        safeCurrentZ + deltaZ * fraction
    )
}

internal val BANK_EMPLOYEE_WAYPOINTS = listOf(
    ActorWaypoint(34f, 18f, 2.0f),
    ActorWaypoint(48f, 18f, 0.8f),
    ActorWaypoint(56f, 18f, 7.0f),
    ActorWaypoint(56f, 40f, 0.8f),
    ActorWaypoint(48f, 40f, 1.2f),
    ActorWaypoint(34f, 18f, 5.0f)
)

internal val MARKET_FARMER_WAYPOINTS = listOf(
    ActorWaypoint(15f, -20f, 1.5f),
    ActorWaypoint(15f, -36f, 0.8f),
    ActorWaypoint(28f, -36f, 0.8f),
    ActorWaypoint(28f, -22f, 7.0f),
    ActorWaypoint(15f, -20f, 4.0f)
)

/** Meera's home → market frontage → Ganga promenade loop, kept clear of static geometry. */
internal val MEERA_DAILY_ROUTE = listOf(
    ActorWaypoint(-14f, 34f, 1.2f),
    ActorWaypoint(-8f, 5f, 0.5f),
    ActorWaypoint(30f, -18f, 2.5f),
    ActorWaypoint(32f, -40f, 0.8f),
    ActorWaypoint(-25f, -45f, 5.0f),
    ActorWaypoint(-30f, -5f, 0.8f),
    ActorWaypoint(-17f, 15f, 2.0f)
)

internal data class WorldCollider(val minX: Float, val maxX: Float, val minZ: Float, val maxZ: Float)

internal val PATNA_COLLIDERS = listOf(
    WorldCollider(-38f, -20f, 16f, 32f),   // riverside home
    WorldCollider(-62f, -42f, -12f, 4f),   // second home
    WorldCollider(51f, 61f, -37f, -19f),   // Golghar
    WorldCollider(52f, 108f, -48f, -4f),   // mixed-use high-rise
    WorldCollider(62f, 78f, 19f, 37f),     // bank
    WorldCollider(-93f, -59f, 16f, 42f),   // university
    WorldCollider(73f, 115f, -72f, -36f),  // power plant
    WorldCollider(24f, 32f, 15f, 21f),     // bus shelter
    WorldCollider(19f, 27f, -33f, -23f),   // market stand
    WorldCollider(17f, 23f, 63f, 70f),     // parked traffic 1
    WorldCollider(62f, 70f, -80f, -72f),  // parked traffic 2
    WorldCollider(-52f, -44f, -92f, -84f), // parked traffic 3
    WorldCollider(-22.05f, -17.95f, -43.29f, -42.71f) // Ganga promenade bench
)


/**
 * Checks a closed pedestrian route against the same static world colliders used by player
 * movement. Expanded AABBs provide a conservative body-clearance margin.
 */
internal fun pedestrianRouteAvoidsColliders(
    route: List<ActorWaypoint>,
    colliders: List<WorldCollider>,
    clearance: Float
): Boolean {
    if (route.size < 2 || !clearance.isFinite() || clearance < 0f) return false
    if (route.any { !it.x.isFinite() || !it.z.isFinite() }) return false
    if (colliders.any {
            !it.minX.isFinite() || !it.maxX.isFinite() || !it.minZ.isFinite() || !it.maxZ.isFinite() ||
                it.minX > it.maxX || it.minZ > it.maxZ
        }
    ) return false

    return route.indices.all { index ->
        val start = route[index]
        val end = route[(index + 1) % route.size]
        colliders.none { obstacle ->
            val minX = obstacle.minX - clearance
            val maxX = obstacle.maxX + clearance
            val minZ = obstacle.minZ - clearance
            val maxZ = obstacle.maxZ + clearance
            val dx = end.x - start.x
            val dz = end.z - start.z
            var enter = 0f
            var exit = 1f

            fun clip(p: Float, q: Float): Boolean {
                if (kotlin.math.abs(p) < 0.000001f) return q >= 0f
                val ratio = q / p
                if (p < 0f) {
                    if (ratio > exit) return false
                    enter = maxOf(enter, ratio)
                } else {
                    if (ratio < enter) return false
                    exit = minOf(exit, ratio)
                }
                return true
            }

            // Liang–Barsky segment clipping against the clearance-expanded rectangle.
            clip(-dx, start.x - minX) &&
                clip(dx, maxX - start.x) &&
                clip(-dz, start.z - minZ) &&
                clip(dz, maxZ - start.z) &&
                enter <= exit
        }
    }
}

private fun hitsWorld(x: Float, z: Float, radius: Float): Boolean =
    x - radius < -480f || x + radius > 480f || z - radius < -480f || z + radius > 480f ||
        PATNA_COLLIDERS.any { x + radius > it.minX && x - radius < it.maxX && z + radius > it.minZ && z - radius < it.maxZ }

/**
 * A third-person, real-time 3D vertical slice set in a fictional 2047 expansion of Patna. This feature branch is APK-build validated by CI.
 * Characters and city props are real textured GLB assets; only the Golghar landmark and
 * terrain/roads are generated geometry. The original Alchemist game remains in the repository.
 */
@Composable
fun Patna2047Screen(modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val saveScope = rememberCoroutineScope()
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = -14f, y = 3.4f, z = 29f)
        lookAt(Position(x = -14f, y = 1f, z = 22f))
        far = 1400f
    }
    val sceneNodes = rememberNodes()
    val input = remember { PatnaInput() }
    var loading by remember { mutableStateOf(true) }
    var worldReady by remember { mutableStateOf(false) }
    var loadMessage by remember { mutableStateOf("Preparing Patna 2047…") }
    var dialogue by remember { mutableStateOf("Ganga ke kinare ek nayi subah…") }
    var objective by remember { mutableStateOf("Explore the riverfront and meet Meera.") }
    var showJournal by remember { mutableStateOf(false) }
    var hasMet by remember { mutableStateOf(false) }
    var saveReady by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf("LOADING") }
    var playerVehicleLoading by remember { mutableStateOf(true) }
    fun persistGameState() {
        if (!saveReady || !worldReady || playerVehicleLoading) return
        saveScope.launch {
            saveStatus = if (context.savePatna2047Save(input.snapshot(hasMet))) "SAVED" else "SAVE ERROR"
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) persistGameState()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            persistGameState()
        }
    }
    var nearHome by remember { mutableStateOf(false) }
    var nearGolghar by remember { mutableStateOf(false) }
    var nearCar by remember { mutableStateOf(false) }
    var playerNode by remember { mutableStateOf<ModelNode?>(null) }
    var npcNode by remember { mutableStateOf<ModelNode?>(null) }
    var pedestrianRoutines by remember { mutableStateOf<List<PedestrianRoutine>>(emptyList()) }
    var vehicleNode by remember { mutableStateOf<ModelNode?>(null) }
    var trafficNodes by remember { mutableStateOf<List<ModelNode>>(emptyList()) }
    var backgroundAssetsCompleted by remember { mutableStateOf(0) }
    var backgroundAssetsFailed by remember { mutableStateOf(0) }
    var playerVehicleUnavailable by remember { mutableStateOf(false) }
    var backgroundAssetsFinished by remember { mutableStateOf(false) }

    val grass = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF71835C), roughness = 0.96f) }
    val asphalt = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF343A40), roughness = 0.9f) }
    val sidewalk = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFB4AA99), roughness = 0.94f) }
    val water = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF347F9A), metallic = 0.18f, roughness = 0.23f) }
    val ghatStone = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFB78E68), roughness = 0.88f) }
    val golgharMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFC6A47B), roughness = 0.93f) }
    val darkStone = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF604E40), roughness = 0.9f) }
    val roadPaint = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFE8DDB7), roughness = 0.7f) }
    val parkGreen = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF506E48), roughness = 0.96f) }
    val skyBlue = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF83B9DF), roughness = 1f) }

    LaunchedEffect(Unit) {
        // Restore the small offline game save before placing the player and vehicle.
        val restoredSave = context.loadPatna2047Save()
        val safeSave = (restoredSave ?: PatnaSaveState()).sanitized()
        val resumeDrivingAfterVehicleLoad = safeSave.driving
        input.restore(safeSave)
        // Keep the first playable frame on foot; restore driving only after the SUV exists.
        input.driving = false
        hasMet = safeSave.hasMet
        objective = if (safeSave.hasMet) {
            "Story 01 complete. Explore Golghar next."
        } else {
            "Explore the riverfront and meet Meera."
        }
        dialogue = if (safeSave.hasMet) {
            "Welcome back to Patna 2047. Your riverfront progress is saved."
        } else {
            "Ganga ke kinare ek nayi subah…"
        }
        saveStatus = if (restoredSave == null) "NEW SAVE" else "READY"
        saveReady = true

        // First render the complete land, roads and river so the player sees the world immediately.
        sceneNodes += PlaneNode(engine, size = Size(x = 1000f, y = 0f, z = 1000f), normal = Direction(y = 1f), materialInstance = grass)
        // Blue sky enclosure replaces the black void around the mobile 3D scene.
        sceneNodes += PlaneNode(engine, size = Size(x = 1000f, y = 520f, z = 0f), normal = Direction(z = 1f), materialInstance = skyBlue).apply { position = Position(x = 0f, y = 260f, z = -490f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 1000f, y = 520f, z = 0f), normal = Direction(z = -1f), materialInstance = skyBlue).apply { position = Position(x = 0f, y = 260f, z = 490f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 0f, y = 520f, z = 1000f), normal = Direction(x = 1f), materialInstance = skyBlue).apply { position = Position(x = -490f, y = 260f, z = 0f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 0f, y = 520f, z = 1000f), normal = Direction(x = -1f), materialInstance = skyBlue).apply { position = Position(x = 490f, y = 260f, z = 0f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 1000f, y = 0f, z = 1000f), normal = Direction(y = -1f), materialInstance = skyBlue).apply { position = Position(x = 0f, y = 520f, z = 0f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 240f, y = 0f, z = 48f), normal = Direction(y = 1f), materialInstance = water).apply { position = Position(x = 0f, y = 0.035f, z = -96f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 12f, y = 0f, z = 188f), normal = Direction(y = 1f), materialInstance = asphalt).apply { position = Position(x = 0f, y = 0.07f, z = 10f) }
        // A paved lay-by beside the main avenue makes the player's SUV visible at spawn.
        sceneNodes += PlaneNode(engine, size = Size(x = 16f, y = 0f, z = 10f), normal = Direction(y = 1f), materialInstance = asphalt).apply { position = Position(x = -12f, y = 0.075f, z = 18f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 800f, y = 0f, z = 10f), normal = Direction(y = 1f), materialInstance = asphalt).apply { position = Position(x = 45f, y = 0.075f, z = 24f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 800f, y = 0f, z = 8f), normal = Direction(y = 1f), materialInstance = sidewalk).apply { position = Position(x = 45f, y = 0.09f, z = 33f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 800f, y = 0f, z = 8f), normal = Direction(y = 1f), materialInstance = sidewalk).apply { position = Position(x = 45f, y = 0.09f, z = 15f) }
        // An extended street grid: no invisible wall after one short block.
        for (z in listOf(-220f, -110f, 140f, 250f, 360f)) {
            sceneNodes += PlaneNode(engine, size = Size(x = 900f, y = 0f, z = 8f), normal = Direction(y = 1f), materialInstance = asphalt).apply { position = Position(x = 0f, y = 0.07f, z = z) }
        }
        for (x in listOf(-180f, 180f, 360f)) {
            sceneNodes += PlaneNode(engine, size = Size(x = 8f, y = 0f, z = 900f), normal = Direction(y = 1f), materialInstance = asphalt).apply { position = Position(x = x, y = 0.07f, z = 0f) }
        }
        sceneNodes += PlaneNode(engine, size = Size(x = 150f, y = 0f, z = 7f), normal = Direction(y = 1f), materialInstance = sidewalk).apply { position = Position(x = -3f, y = 0.09f, z = -57f) }
        sceneNodes += PlaneNode(engine, size = Size(x = 150f, y = 0f, z = 4f), normal = Direction(y = 1f), materialInstance = asphalt).apply { position = Position(x = -3f, y = 0.08f, z = -65f) }
        // Dash markings and a broad river crossing.
        for (i in -8..8) {
            sceneNodes += PlaneNode(engine, size = Size(x = 0.22f, y = 0f, z = 3.4f), normal = Direction(y = 1f), materialInstance = roadPaint).apply { position = Position(x = 0f, y = 0.095f, z = i * 10f + 10f) }
        }
        sceneNodes += PlaneNode(engine, size = Size(x = 9f, y = 0f, z = 60f), normal = Direction(y = 1f), materialInstance = asphalt).apply { position = Position(x = 0f, y = 0.14f, z = -96f) }
        for (i in 0..5) {
            sceneNodes += PlaneNode(engine, size = Size(x = 42f - i * 2f, y = 0f, z = 1.45f), normal = Direction(y = 1f), materialInstance = ghatStone).apply { position = Position(x = -58f + i * 1.1f, y = 0.12f + i * 0.08f, z = -69f - i * 2.1f) }
        }
        sceneNodes += PlaneNode(engine, size = Size(x = 60f, y = 0f, z = 28f), normal = Direction(y = 1f), materialInstance = parkGreen).apply { position = Position(x = -25f, y = 0.055f, z = -37f) }
        // A recognisable Golghar silhouette made as one tapered, shaded mesh rather than primitives.
        val golghar = createGolghar(engine, golgharMaterial)
        golghar.position = Position(x = 56f, y = 0.1f, z = -28f)
        sceneNodes += golghar
        sceneNodes += PlaneNode(engine, size = Size(x = 1.8f, y = 2.4f, z = 0f), normal = Direction(z = 1f), materialInstance = darkStone).apply { position = Position(x = 56f, y = 1.2f, z = -19.65f) }
        loadMessage = "Loading people, homes and city assets…"

        suspend fun addModel(
            url: String,
            scale: Float,
            at: Position,
            yaw: Float = 0f,
            walk: Boolean = false,
            targetHeightMeters: Float? = null
        ): ModelNode? {
            return try {
                val cachedLocation = PatnaAssetCache.resolve(
                    context = context,
                    url = url,
                    keepOffline = url == MAN_GLB || url == WOMAN_GLB
                ) ?: run {
                    loadMessage = "Asset unavailable; continuing with the local Patna world…"
                    return null
                }
                val instance = modelLoader.loadModelInstance(cachedLocation)
                if (instance == null) {
                    loadMessage = "Some city assets are unavailable. Continuing with the 3D city…"
                    null
                } else {
                    ModelNode(
                        instance,
                        autoAnimate = false,
                        // scaleToUnits fits a cube, not a human's vertical stature. For people,
                        // start from the asset's native AABB and scale from its measured Y height.
                        scaleToUnits = if (targetHeightMeters == null) scale else null
                    ).also { node ->
                        if (targetHeightMeters != null) {
                            val nativeHeightMeters = node.halfExtent.y * 2f
                            val uniformScale = uniformScaleForCharacterHeight(nativeHeightMeters, targetHeightMeters)
                            if (uniformScale.isFinite() && uniformScale > 0f) {
                                node.scale = Scale(uniformScale)
                            }
                        }
                        node.rotation = Rotation(y = yaw)
                        // Anchor the full transformed AABB bottom-center at the requested world point.
                        val grounding = node.characterGrounding()
                        node.position = grounding.rootPosition(at.x, at.y, at.z, yaw)
                        if (targetHeightMeters != null && BuildConfig.DEBUG) {
                            val staticHeightMeters = node.halfExtent.y * 2f * node.scale.y
                            val staticBottomY = node.position.y + grounding.localBottomY
                            val staticTopY = node.position.y + (node.center.y + node.halfExtent.y) * node.scale.y
                            Log.d(
                                "Patna2047Scale",
                                "asset=" + url.substringAfterLast('/') + " nativeY=" + (node.halfExtent.y * 2f) + "m " +
                                    "scaleY=" + node.scale.y + " targetY=" + targetHeightMeters + "m " +
                                    "staticAabbHeight=" + staticHeightMeters + "m worldY=[" + staticBottomY + "," + staticTopY + "]m " +
                                    "yaw=" + yaw + " animations=" + node.animationCount + "; animated skeleton extremes are not included"
                            )
                        }
                        if (walk && node.animationCount > 0) node.playAnimation(0, 0.95f, true)
                        sceneNodes += node
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loadMessage = "Network asset skipped; the rest of Patna is still playable."
                null
            }
        }

        // Ghat benches and riverfront details remain available in the first static world frame.
        sceneNodes += CubeNode(engine, size = Size(x = 4.1f, y = 0.48f, z = 0.58f), materialInstance = darkStone).apply { position = Position(x = -20f, y = 0.52f, z = -43f) }
        sceneNodes += CubeNode(engine, size = Size(x = 0.14f, y = 1.15f, z = 0.14f), materialInstance = darkStone).apply { position = Position(x = -21.7f, y = 0.55f, z = -43f) }
        sceneNodes += CubeNode(engine, size = Size(x = 0.14f, y = 1.15f, z = 0.14f), materialInstance = darkStone).apply { position = Position(x = -18.3f, y = 0.55f, z = -43f) }

        // Warm street lamps are generated locally and do not wait for GLB downloads.
        for (x in listOf(-8f, 8f, 24f, 42f, 60f, 78f)) {
            sceneNodes += CylinderNode(engine, radius = 0.08f, height = 5.8f, materialInstance = darkStone).apply { position = Position(x = x, y = 2.9f, z = 10f) }
            sceneNodes += CubeNode(engine, size = Size(x = 0.8f, y = 0.12f, z = 0.12f), materialInstance = roadPaint).apply { position = Position(x = x + 0.28f, y = 5.68f, z = 10f) }
        }

        // Load the two essential on-foot actors and the player's SUV before enabling controls.
        // Homes, workers, traffic and skyline props stream in after the first playable frame.
        loadMessage = "Loading Aarav, Meera and the SUV…"
        val boy = addModel(MAN_GLB, 1f, Position(x = -18f, y = 0.12f, z = 18f), yaw = 180f, walk = true, targetHeightMeters = 1.72f)
        val girl = addModel(WOMAN_GLB, 1f, Position(x = -14f, y = 0.12f, z = 34f), yaw = 180f, walk = true, targetHeightMeters = 1.64f)

        playerNode = boy
        npcNode = girl
        vehicleNode = null
        playerVehicleLoading = true
        playerVehicleUnavailable = false

        if (boy != null && girl != null) {
            dialogue = "Aarav aur Meera ghar se nikal pade. Ganga ghat par aaj mela hai."
            objective = "Walk to the Ganga riverfront. Tap INTERACT near Meera."
            if (playerVehicleUnavailable) {
                dialogue = "Aarav and Meera are ready. The SUV is unavailable, so explore on foot."
            }
            worldReady = true
            loading = false
            loadMessage = "Patna 2047 • Ganga Riverfront"

            launch {
                // Vehicle loading is optional: walking becomes playable before the SUV download.
                val playerCar = addModel(
                    SUV_GLB,
                    4.7f,
                    Position(x = input.carX, y = 0.12f, z = input.carZ),
                    yaw = 0f
                )
                vehicleNode = playerCar
                playerVehicleLoading = false
                playerVehicleUnavailable = playerCar == null
                if (playerCar != null && resumeDrivingAfterVehicleLoad) {
                    input.driving = true
                } else if (playerCar == null && resumeDrivingAfterVehicleLoad) {
                    // Fall back to the last saved vehicle position without a phantom driving state.
                    input.playerX = input.carX
                    input.playerZ = input.carZ
                    input.driving = false
                }
                if (playerCar == null) {
                    dialogue = "Aarav and Meera are ready. The SUV is unavailable, so explore on foot."
                }
                persistGameState()

                suspend fun addBackgroundAsset(
                    url: String,
                    scale: Float,
                    at: Position,
                    yaw: Float = 0f,
                    walk: Boolean = false,
                    targetHeightMeters: Float? = null
                ): ModelNode? {
                    val node = addModel(url, scale, at, yaw, walk, targetHeightMeters)
                    backgroundAssetsCompleted = (backgroundAssetsCompleted + 1).coerceAtMost(BACKGROUND_CITY_ASSET_COUNT)
                    if (node == null) backgroundAssetsFailed += 1
                    return node
                }

                // Non-essential detail is deliberately loaded after controls are live.
                addBackgroundAsset(HOUSE_GLB, 17f, Position(x = -28f, y = 0f, z = 24f), yaw = 90f)
                addBackgroundAsset(HOUSE_GLB, 14f, Position(x = -52f, y = 0f, z = -4f), yaw = 0f)

                val businessman = addBackgroundAsset(
                    BUSINESSMAN_GLB, 1f, Position(x = 34f, y = 0.12f, z = 18f),
                    yaw = 180f, walk = true, targetHeightMeters = 1.76f
                )
                val farmer = addBackgroundAsset(
                    FARMER_GLB, 1f, Position(x = 15f, y = 0.12f, z = -20f),
                    yaw = 90f, walk = true, targetHeightMeters = 1.68f
                )
                pedestrianRoutines = listOfNotNull(
                    businessman?.let {
                        PedestrianRoutine(it, BANK_EMPLOYEE_WAYPOINTS, 0.90f, it.characterGrounding(), 34f, 18f, 180f)
                    },
                    farmer?.let {
                        PedestrianRoutine(it, MARKET_FARMER_WAYPOINTS, 0.82f, it.characterGrounding(), 15f, -20f, 90f)
                    }
                )

                // Parked and moving traffic are streamed after the core player vehicle.
                addBackgroundAsset(SUV_GLB, 4.7f, Position(x = 20f, y = 0.12f, z = 66f), yaw = 0f)
                addBackgroundAsset(SUV_GLB, 4.7f, Position(x = 66f, y = 0.12f, z = -76f), yaw = 180f)
                addBackgroundAsset(SUV_GLB, 4.7f, Position(x = -48f, y = 0.12f, z = -88f), yaw = 90f)
                val trafficA = addBackgroundAsset(SUV_GLB, 4.7f, Position(x = 4f, y = 0.12f, z = 180f), yaw = 0f)
                val trafficB = addBackgroundAsset(SUV_GLB, 4.7f, Position(x = 180f, y = 0.12f, z = -180f), yaw = 180f)
                trafficNodes = listOfNotNull(trafficA, trafficB)

                addBackgroundAsset(BANK_GLB, 18f, Position(x = 70f, y = 0f, z = 28f), yaw = -90f)
                addBackgroundAsset(UNIVERSITY_GLB, 26f, Position(x = -76f, y = 0f, z = 29f), yaw = 0f)
                addBackgroundAsset(POWER_PLANT_GLB, 34f, Position(x = 94f, y = 0f, z = -54f), yaw = 90f)
                addBackgroundAsset(TREE_GLB, 8.5f, Position(x = -43f, y = 0.08f, z = -28f))
                addBackgroundAsset(STREETLIGHT_GLB, 8f, Position(x = -4.5f, y = 0.08f, z = 2f))
                addBackgroundAsset(BUSSTOP_GLB, 3.0f, Position(x = 28f, y = 0.08f, z = 18f), yaw = 90f)
                addBackgroundAsset(FARM_STAND_GLB, 2.6f, Position(x = 23f, y = 0.08f, z = -28f), yaw = 180f)

                // The 23 MB skyline tower is intentionally last so it cannot delay the first playable frame.
                addBackgroundAsset(HIGHRISE_GLB, 118f, Position(x = 80f, y = 0f, z = -26f), yaw = 0f)
                backgroundAssetsFinished = true
            }
        } else {
            // Keep the generated riverfront visible, but never report a playable world without both actors.
            worldReady = false
            loading = false
            playerVehicleLoading = false
            playerVehicleUnavailable = true
            loadMessage = "Aarav or Meera could not be downloaded. Connect to the internet and reopen Patna 2047."
            dialogue = "The riverfront is ready, but the character models are unavailable."
            objective = "Reconnect to download the two required character models."
        }
    }

    // Meera follows a home → market → Ganga promenade → home daily routine.
    var nearNpc by remember { mutableStateOf(false) }
    val npcRoute = remember { MEERA_DAILY_ROUTE }
    // Read streamed assets without restarting the movement loop or resetting Meera's route.
    val latestVehicleNode = rememberUpdatedState(vehicleNode)
    val latestPedestrianRoutines = rememberUpdatedState(pedestrianRoutines)
    val latestTrafficNodes = rememberUpdatedState(trafficNodes)

    LaunchedEffect(worldReady, playerNode, npcNode) {
        val player = playerNode ?: return@LaunchedEffect
        val npc = npcNode ?: return@LaunchedEffect
        if (!worldReady) return@LaunchedEffect
        val playerGrounding = player.characterGrounding()
        val npcGrounding = npc.characterGrounding()
        var playerYawDegrees = 180f
        var playerVelocityX = 0f
        var playerVelocityZ = 0f
        // These are world-space foot anchors; ModelNode.position is the transformed GLB pivot.
        var npcX = -14f
        var npcZ = 34f
        var npcYawDegrees = 180f
        var previous = 0L
        var saveAccumulator = 0f
        var px = input.playerX
        var pz = input.playerZ
        var segment = 0
        var pause = 0f
        var lastNear = false
        var lastNearHome = false
        var lastNearGolghar = false
        var lastWalkAnimationSpeed = -1f
        var lastNpcWalking: Boolean? = null
        // Cache wheel renderables as streamed vehicle models arrive; do not restart the frame loop.
        var activeVehicleNode = latestVehicleNode.value
        var wheelNodes = activeVehicleNode?.renderableNodes?.filter { renderable ->
            val partName = renderable.name.orEmpty().lowercase()
            partName.contains("wheel") || partName.contains("tire") || partName.contains("tyre")
        }.orEmpty()
        var wheelRotation = 0f

        // These arrays grow when background worker models finish loading.
        var activePedestrianRoutines = latestPedestrianRoutines.value
        var workerSegments = IntArray(activePedestrianRoutines.size)
        var workerPauses = FloatArray(activePedestrianRoutines.size)
        var workerLastWalking = BooleanArray(activePedestrianRoutines.size) { true }

        var activeTrafficNodes = latestTrafficNodes.value
        var trafficWheelNodes = activeTrafficNodes.map { car ->
            car.renderableNodes.filter { renderable ->
                val partName = renderable.name.orEmpty().lowercase()
                partName.contains("wheel") || partName.contains("tire") || partName.contains("tyre")
            }
        }
        var trafficWheelAngles = FloatArray(activeTrafficNodes.size)
        while (true) {
            withFrameNanos { now ->
                val dt = if (previous == 0L) 0.016f else ((now - previous) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                previous = now

                val currentVehicle = latestVehicleNode.value
                if (currentVehicle !== activeVehicleNode) {
                    activeVehicleNode = currentVehicle
                    wheelNodes = currentVehicle?.renderableNodes?.filter { renderable ->
                        val partName = renderable.name.orEmpty().lowercase()
                        partName.contains("wheel") || partName.contains("tire") || partName.contains("tyre")
                    }.orEmpty()
                    wheelRotation = 0f
                }

                val currentRoutines = latestPedestrianRoutines.value
                if (currentRoutines !== activePedestrianRoutines) {
                    activePedestrianRoutines = currentRoutines
                    workerSegments = IntArray(currentRoutines.size)
                    workerPauses = FloatArray(currentRoutines.size)
                    workerLastWalking = BooleanArray(currentRoutines.size) { true }
                }

                val currentTrafficNodes = latestTrafficNodes.value
                if (currentTrafficNodes !== activeTrafficNodes) {
                    activeTrafficNodes = currentTrafficNodes
                    trafficWheelNodes = currentTrafficNodes.map { car ->
                        car.renderableNodes.filter { renderable ->
                            val partName = renderable.name.orEmpty().lowercase()
                            partName.contains("wheel") || partName.contains("tire") || partName.contains("tyre")
                        }
                    }
                    trafficWheelAngles = FloatArray(currentTrafficNodes.size)
                }

                saveAccumulator += dt
                if (saveAccumulator >= 5f) {
                    saveAccumulator = 0f
                    persistGameState()
                }

                val vehicle = currentVehicle
                if (input.driving && vehicle != null) {
                    // The on-foot velocity must not leak into the next exit-from-car movement.
                    playerVelocityX = 0f
                    playerVelocityZ = 0f
                    lastWalkAnimationSpeed = -1f
                    // Mobile driving: throttle, reverse/brake, steering and coasting.
                    input.carSpeed = when {
                        input.forward -> (input.carSpeed + 10.5f * dt).coerceAtMost(24f)
                        input.back -> (input.carSpeed - 13.5f * dt).coerceAtLeast(-7f)
                        else -> input.carSpeed * (1f - (1.45f * dt).coerceAtMost(0.9f))
                    }
                    val steering = (if (input.right) 1f else 0f) - (if (input.left) 1f else 0f)
                    if (kotlin.math.abs(input.carSpeed) > 0.2f) {
                        val travelDirection = if (input.carSpeed >= 0f) 1f else -1f
                        input.carYaw -= steering * 1.15f * dt * travelDirection
                    }
                    // The SUV asset's forward axis is -Z; motion and model yaw now match.
                    val nextCarX = input.carX - sin(input.carYaw) * input.carSpeed * dt
                    val nextCarZ = input.carZ - cos(input.carYaw) * input.carSpeed * dt
                    val trafficHit = currentTrafficNodes.any { other ->
                        sqrt((nextCarX - other.position.x) * (nextCarX - other.position.x) + (nextCarZ - other.position.z) * (nextCarZ - other.position.z)) < 4.8f
                    }
                    val pedestrianHit = (listOf(npcX to npcZ) + currentRoutines.map { it.x to it.z }).any { (personX, personZ) ->
                        sqrt((nextCarX - personX) * (nextCarX - personX) + (nextCarZ - personZ) * (nextCarZ - personZ)) < 2.8f
                    }
                    if (hitsWorld(nextCarX, nextCarZ, 2.5f) || trafficHit || pedestrianHit) {
                        input.carSpeed = 0f
                    } else {
                        input.carX = nextCarX
                        input.carZ = nextCarZ
                    }
                    vehicle.position = Position(x = input.carX, y = 0.12f, z = input.carZ)
                    vehicle.rotation = Rotation(y = input.carYaw * 180f / PI.toFloat())
                    wheelRotation += input.carSpeed * dt / 0.37f * 180f / PI.toFloat()
                    wheelNodes.forEach { wheel ->
                        val partName = wheel.name.orEmpty().lowercase()
                        val frontWheel = partName.contains("front") || partName.contains("fl") || partName.contains("fr")
                        val steeringAngle = if (frontWheel) steering * 12f else 0f
                        wheel.rotation = Rotation(x = wheelRotation, y = steeringAngle)
                    }
                    px = input.carX
                    pz = input.carZ
                    player.position = Position(x = px, y = -35f, z = pz)
                    player.setAnimationSpeed(0, 0.12f)
                } else {
                    // Re-sync the local simulation after the player exits the car.
                    px = input.playerX
                    pz = input.playerZ
                    val moveX = (if (input.right) 1f else 0f) - (if (input.left) 1f else 0f)
                    val moveZ = (if (input.forward) 1f else 0f) - (if (input.back) 1f else 0f)
                    val inputLength = sqrt(moveX * moveX + moveZ * moveZ)
                    val hasMovementInput = inputLength > 0.001f
                    // Screen-up is forward, screen-left is left, relative to the camera heading.
                    val worldX = moveX * cos(input.yaw) - moveZ * sin(input.yaw)
                    val worldZ = -moveX * sin(input.yaw) - moveZ * cos(input.yaw)
                    val targetVelocityX = if (hasMovementInput) worldX * 1.55f / inputLength else 0f
                    val targetVelocityZ = if (hasMovementInput) worldZ * 1.55f / inputLength else 0f
                    // A short acceleration ramp and slightly stronger braking remove instant starts/stops.
                    val acceleration = if (hasMovementInput) 4.8f else 6.5f
                    val velocity = approachPlanarVelocity(
                        playerVelocityX,
                        playerVelocityZ,
                        targetVelocityX,
                        targetVelocityZ,
                        acceleration * dt
                    )
                    playerVelocityX = velocity.x
                    playerVelocityZ = velocity.z

                    // Axis-separated collision response keeps sliding along walls and parked objects.
                    fun clearOfPeopleAndTraffic(x: Float, z: Float): Boolean {
                        val peopleClear = (listOf(npcX to npcZ) + currentRoutines.map { it.x to it.z }).all { (personX, personZ) ->
                            sqrt((x - personX) * (x - personX) + (z - personZ) * (z - personZ)) > 1.15f
                        }
                        val trafficClear = currentTrafficNodes.all { car ->
                            sqrt((x - car.position.x) * (x - car.position.x) + (z - car.position.z) * (z - car.position.z)) > 4.0f
                        }
                        return peopleClear && trafficClear
                    }
                    val nextX = px + playerVelocityX * dt
                    val nextZ = pz + playerVelocityZ * dt
                    if (kotlin.math.abs(playerVelocityX) > 0.0001f) {
                        if (!hitsWorld(nextX, pz, 0.45f) && clearOfPeopleAndTraffic(nextX, pz) &&
                            sqrt((nextX - input.carX) * (nextX - input.carX) + (pz - input.carZ) * (pz - input.carZ)) > 3.0f
                        ) px = nextX else playerVelocityX = 0f
                    }
                    if (kotlin.math.abs(playerVelocityZ) > 0.0001f) {
                        if (!hitsWorld(px, nextZ, 0.45f) && clearOfPeopleAndTraffic(px, nextZ) &&
                            sqrt((px - input.carX) * (px - input.carX) + (nextZ - input.carZ) * (nextZ - input.carZ)) > 3.0f
                        ) pz = nextZ else playerVelocityZ = 0f
                    }

                    val playerSpeed = sqrt(playerVelocityX * playerVelocityX + playerVelocityZ * playerVelocityZ)
                    val walkAnimationSpeed = if (playerSpeed < 0.05f) 0f else {
                        0.5f + 0.45f * (playerSpeed / 1.55f).coerceIn(0f, 1f)
                    }
                    if (player.animationCount > 0 &&
                        (lastWalkAnimationSpeed < 0f || kotlin.math.abs(walkAnimationSpeed - lastWalkAnimationSpeed) >= 0.05f)
                    ) {
                        player.setAnimationSpeed(0, walkAnimationSpeed)
                        lastWalkAnimationSpeed = walkAnimationSpeed
                    }
                    if (playerSpeed > 0.05f) {
                        val targetPlayerYaw = kotlin.math.atan2(-playerVelocityX, -playerVelocityZ) * 180f / PI.toFloat() + 180f
                        playerYawDegrees = approachYawDegrees(playerYawDegrees, targetPlayerYaw, 540f * dt)
                    }
                    input.playerX = px
                    input.playerZ = pz
                    player.rotation = Rotation(y = playerYawDegrees)
                    player.position = playerGrounding.rootPosition(px, CHARACTER_GROUND_Y, pz, playerYawDegrees)
                }

                // Independent orbit camera: full 360° yaw and adjustable elevation.
                val followX = if (input.driving) input.carX else px
                val followZ = if (input.driving) input.carZ else pz
                val followDistance = if (input.driving) 9.5f else 6.0f
                cameraNode.position = Position(
                    x = followX + sin(input.yaw) * followDistance,
                    y = input.cameraHeight,
                    z = followZ + cos(input.yaw) * followDistance
                )
                cameraNode.lookAt(Position(x = followX, y = input.cameraLookHeight, z = followZ))

                val target = npcRoute[segment]
                val nx = target.x - npcX
                val nz = target.z - npcZ
                val distance = sqrt(nx * nx + nz * nz)
                val npcWalking = distance >= 0.65f
                if (lastNpcWalking != npcWalking) {
                    if (npc.animationCount > 0) npc.setAnimationSpeed(0, if (npcWalking) 0.9f else 0f)
                    lastNpcWalking = npcWalking
                }
                if (distance < 0.65f) {
                    if (pause < target.pause) pause += dt
                    else {
                        pause = 0f
                        segment = (segment + 1) % npcRoute.size
                    }
                } else {
                    val step = (1.3f * dt).coerceAtMost(distance)
                    npcX += nx / distance * step
                    npcZ += nz / distance * step
                    val targetNpcYaw = kotlin.math.atan2(nx, nz) * 180f / PI.toFloat() + 180f
                    npcYawDegrees = approachYawDegrees(npcYawDegrees, targetNpcYaw, 360f * dt)
                }
                npc.rotation = Rotation(y = npcYawDegrees)
                npc.position = npcGrounding.rootPosition(npcX, CHARACTER_GROUND_Y, npcZ, npcYawDegrees)

                // Autonomous cars cruise long avenue loops with a fixed lane and speed.
                currentTrafficNodes.getOrNull(0)?.let { traffic ->
                    val trafficSpeed = 6.5f
                    val nextZ = traffic.position.z - trafficSpeed * dt
                    traffic.position = Position(x = 4f, y = 0.12f, z = if (nextZ < -180f) 180f else nextZ)
                    traffic.rotation = Rotation(y = 0f)
                    trafficWheelAngles[0] += trafficSpeed * dt / 0.37f * 180f / PI.toFloat()
                    trafficWheelNodes.getOrNull(0).orEmpty().forEach { it.rotation = Rotation(x = trafficWheelAngles[0]) }
                }
                currentTrafficNodes.getOrNull(1)?.let { traffic ->
                    val trafficSpeed = 7.2f
                    val nextZ = traffic.position.z + trafficSpeed * dt
                    traffic.position = Position(x = 180f, y = 0.12f, z = if (nextZ > 180f) -180f else nextZ)
                    traffic.rotation = Rotation(y = 180f)
                    trafficWheelAngles[1] -= trafficSpeed * dt / 0.37f * 180f / PI.toFloat()
                    trafficWheelNodes.getOrNull(1).orEmpty().forEach { it.rotation = Rotation(x = trafficWheelAngles[1]) }
                }

                val nowNear = sqrt(
                    (px - npcX) * (px - npcX) +
                        (pz - npcZ) * (pz - npcZ)
                ) < 4.3f
                if (nowNear != lastNear) {
                    lastNear = nowNear
                    nearNpc = nowNear
                }
                val carNearby = !input.driving && sqrt((px - input.carX) * (px - input.carX) + (pz - input.carZ) * (pz - input.carZ)) < 10f
                if (carNearby != nearCar) nearCar = carNearby
                val homeNearby = sqrt((px + 28f) * (px + 28f) + (pz - 24f) * (pz - 24f)) < 7f
                if (homeNearby != lastNearHome) {
                    lastNearHome = homeNearby
                    nearHome = homeNearby
                }
                val golgharNearby = sqrt((px - 56f) * (px - 56f) + (pz + 28f) * (pz + 28f)) < 11f
                // Independent pedestrian routes around the bus stop and market stall.
                currentRoutines.forEachIndexed { index, routine ->
                    val worker = routine.node
                    val route = routine.waypoints
                    val targetWorker = route[workerSegments[index]]
                    val wx = targetWorker.x - routine.x
                    val wz = targetWorker.z - routine.z
                    val workerDistance = sqrt(wx * wx + wz * wz)
                    if (workerDistance < 0.5f) {
                        // Stop the walk cycle at a work/rest stop instead of walking in place.
                        if (workerLastWalking[index]) {
                            if (worker.animationCount > 0) worker.setAnimationSpeed(0, 0f)
                            workerLastWalking[index] = false
                        }
                        if (workerPauses[index] < targetWorker.pause) workerPauses[index] += dt
                        else {
                            workerPauses[index] = 0f
                            workerSegments[index] = (workerSegments[index] + 1) % route.size
                        }
                    } else {
                        if (!workerLastWalking[index]) {
                            if (worker.animationCount > 0) worker.setAnimationSpeed(0, routine.walkAnimationSpeed)
                            workerLastWalking[index] = true
                        }
                        val step = (1.25f * dt).coerceAtMost(workerDistance)
                        routine.x += wx / workerDistance * step
                        routine.z += wz / workerDistance * step
                        val targetWorkerYaw = kotlin.math.atan2(wx, wz) * 180f / PI.toFloat() + 180f
                        routine.yawDegrees = approachYawDegrees(routine.yawDegrees, targetWorkerYaw, 360f * dt)
                    }
                    worker.rotation = Rotation(y = routine.yawDegrees)
                    worker.position = routine.grounding.rootPosition(
                        routine.x,
                        CHARACTER_GROUND_Y,
                        routine.z,
                        routine.yawDegrees
                    )
                }
                if (golgharNearby != lastNearGolghar) {
                    lastNearGolghar = golgharNearby
                    nearGolghar = golgharNearby
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF101B21))) {
        Scene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            cameraNode = cameraNode,
            cameraManipulator = null,
            childNodes = sceneNodes,
            onFrame = { }
        )

        // Drag on the open right side: unbounded 360° orbit; vertical drag raises/lowers view.
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.57f).align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        input.yaw += drag.x * 0.008f
                        // Swipe up raises the look target so the camera can look level/skyward.
                        input.cameraLookHeight = (input.cameraLookHeight - drag.y * 0.034f).coerceIn(0.6f, 22f)
                    }
                }
        )

        Column(
            Modifier.align(Alignment.TopStart).padding(12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xD91A252B)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("PATNA 2047", color = Color(0xFFFFD99A), fontSize = 16.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Text("GANGA RIVERFRONT  •  BIHAR", color = Color(0xFFE5E3DB), fontSize = 9.sp, letterSpacing = 1.3.sp)
            Text(
                when {
                    nearNpc -> "MEERA IS NEARBY  •  TAP INTERACT"
                    nearHome -> "HOME  •  TAP INTERACT"
                    nearGolghar -> "GOLGHAR  •  TAP INTERACT"
                    else -> objective
                },
                color = Color(0xFFB7D6CF), fontSize = 10.sp
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            color = Color(0xD91A252B), shape = RoundedCornerShape(12.dp)
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                Text("LIVE WORLD", color = Color(0xFFFFD99A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        loading -> loadMessage
                        !worldReady -> "CHARACTERS UNAVAILABLE • CITY VIEW ONLY"
                        playerVehicleLoading -> "ON FOOT • SUV LOADING"
                        !backgroundAssetsFinished -> "CITY ASSETS • $backgroundAssetsCompleted/$BACKGROUND_CITY_ASSET_COUNT"
                        playerVehicleUnavailable && backgroundAssetsFailed > 0 -> "ON FOOT • SUV UNAVAILABLE • $backgroundAssetsFailed ASSETS SKIPPED"
                        playerVehicleUnavailable -> "ON FOOT • SUV UNAVAILABLE"
                        backgroundAssetsFailed > 0 -> "CITY READY • $backgroundAssetsFailed ASSETS SKIPPED"
                        else -> "AARAV  •  MEERA  •  BANK STAFF  •  MARKET FARMER"
                    },
                    color = Color.White,
                    fontSize = 9.sp
                )
                Text(if (input.driving) "DRIVING  •  ${(input.carSpeed * 3.6f).toInt()} km/h" else "ON FOOT  •  360° CAMERA", color = Color(0xFFB4C5C3), fontSize = 9.sp)
                Text("Drag right side to orbit / tilt", color = Color(0xFFB4C5C3), fontSize = 9.sp)
                Text("SAVE • $saveStatus", color = Color(0xFFB4C5C3), fontSize = 9.sp)
            }
        }

        // Left thumb D-pad — press and hold to walk.
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                if (input.driving) "▲ GAS  •  ▼ REVERSE/BRAKE  •  ◀ ▶ STEER" else "▲ FORWARD  •  ▼ BACK  •  ◀ ▶ STRAFE",
                color = Color(0xFFFFD99A), fontSize = 9.sp, fontWeight = FontWeight.Bold
            )
            HoldDirection("▲", { input.forward = it }, enabled = worldReady)
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                HoldDirection("◀", { input.left = it }, enabled = worldReady)
                HoldDirection("▼", { input.back = it }, enabled = worldReady)
                HoldDirection("▶", { input.right = it }, enabled = worldReady)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (input.driving) {
                        // Step out on the passenger side and resume walking.
                        val exitX = input.carX + 3.4f
                        val exitZ = input.carZ
                        val exitClear = !hitsWorld(exitX, exitZ, 0.5f)
                        input.playerX = if (exitClear) exitX else input.carX - 3.4f
                        input.playerZ = exitZ
                        input.driving = false
                        input.carSpeed = 0f
                        input.yaw = input.carYaw
                        dialogue = "Aarav: Gaadi park kar di. Ab paidal Meera ke paas chalte hain."
                    } else if (nearCar && vehicleNode != null) {
                        input.driving = true
                        input.carSpeed = 0f
                        input.yaw = input.carYaw
                                                 dialogue = "Aarav: Seat belt laga li. ▲ accelerate, ▼ reverse/brake, left/right steer."
                    }
                    persistGameState()
                },
                enabled = if (input.driving) true else nearCar && vehicleNode != null,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66B5A7), contentColor = Color(0xFF142220)),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (input.driving) "EXIT VEHICLE" else "GET IN CAR", fontWeight = FontWeight.Black, letterSpacing = 0.7.sp) }
            Button(
                onClick = {
                    when {
                        nearNpc -> {
                            hasMet = true
                            dialogue = "Meera: Aarav, ghat par thodi der rukte hain. Aaj Ganga aarti dekhni hai!  •  Aarav: Haan, phir ghar lautenge."
                            objective = "Story 01 complete: Meet Meera. Explore Golghar next."
                            persistGameState()
                        }
                        nearHome -> {
                            dialogue = "Meera: Yeh hamara ghar hai. Chalo, riverfront tak paidal chalte hain."
                            objective = "Walk to Meera at the Ganga promenade."
                        }
                        nearGolghar -> {
                            dialogue = "Aarav: Golghar, Patna ka mashhoor anaaj-gola. Is shehar ki history yahin zinda hai."
                            objective = if (hasMet) "Story 01 complete. Explore the Ganga riverfront." else "Find Meera near the riverfront."
                        }
                        else -> {
                            dialogue = "Aarav: Meera ko riverfront par dhoondhna hai. Ghar se ghat ki taraf chalo."
                        }
                    }
                },
                enabled = worldReady,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE7B66C), contentColor = Color(0xFF242019)),
                shape = RoundedCornerShape(14.dp)
            ) { Text("INTERACT", fontWeight = FontWeight.Black, letterSpacing = 1.sp) }
            Button(
                onClick = { showJournal = !showJournal },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xD91A252B), contentColor = Color.White),
                shape = RoundedCornerShape(12.dp)
            ) { Text(if (showJournal) "CLOSE" else "JOURNAL", fontSize = 10.sp) }
        }

        if (showJournal) {
            Surface(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp).width(260.dp),
                color = Color(0xF21B292C), shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PATNA LIFE • 2047", color = Color(0xFFFFD99A), fontWeight = FontWeight.Black)
                    Text("Home: Aarav & Meera's riverside neighbourhood", color = Color.White, fontSize = 12.sp)
                    Text("Daily routine: home → market → Ganga promenade → home", color = Color(0xFFD4E1DA), fontSize = 11.sp)
                    Text(if (hasMet) "✓ Story 01 — Meet Meera" else "○ Story 01 — Meet Meera", color = Color(0xFFFFD99A), fontSize = 11.sp)
                    Text("Landmark: Golghar • River: Ganga", color = Color(0xFFD4E1DA), fontSize = 11.sp)
                }
            }
        }

        Surface(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            color = Color(0xD91A252B), shape = RoundedCornerShape(12.dp)
        ) {
            Text(dialogue, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), color = Color(0xFFF4EBDC), fontSize = 10.sp, maxLines = 2)
        }

        if (loading || !worldReady) {
            Surface(
                modifier = Modifier.align(Alignment.Center).padding(16.dp),
                color = Color(0xEF19252A), shape = RoundedCornerShape(14.dp)
            ) {
                Text(loadMessage, modifier = Modifier.padding(16.dp), color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HoldDirection(label: String, onHold: (Boolean) -> Unit, enabled: Boolean = true) {
    Box(
        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(15.dp))
            .background(if (enabled) Color(0xCC18252B) else Color(0x66515A5C))
            .pointerInput(enabled) {
                detectTapGestures(onPress = {
                    if (enabled) {
                        onHold(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onHold(false)
                        }
                    }
                })
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFFFFD99A), fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

/** Tapered Golghar-inspired granary mesh, with a gradual shoulder and a narrow crown. */
private fun createGolghar(engine: com.google.android.filament.Engine, material: com.google.android.filament.MaterialInstance): GeometryNode {
    val slices = 56
    val rings = listOf(
        0f to 8.2f,
        2.5f to 7.8f,
        7.0f to 6.8f,
        12.5f to 5.2f,
        17.0f to 3.8f,
        19.0f to 2.6f
    )
    val vertices = buildList {
        rings.forEachIndexed { ringIndex, (y, radius) ->
            for (i in 0..slices) {
                val a = 2.0 * PI * i / slices
                val x = cos(a).toFloat()
                val z = sin(a).toFloat()
                add(
                    Geometry.Vertex(
                        position = Position(x = radius * x, y = y, z = radius * z),
                        normal = Direction(x = x * 0.84f, y = 0.32f, z = z * 0.84f),
                        uvCoordinate = UvCoordinate(i.toFloat() / slices, ringIndex.toFloat() / (rings.size - 1))
                    )
                )
            }
        }
    }
    val indices = buildList {
        for (r in 0 until rings.size - 1) {
            for (i in 0 until slices) {
                val a = r * (slices + 1) + i
                val b = a + slices + 1
                addAll(listOf(a, b, a + 1, a + 1, b, b + 1))
            }
        }
    }
    val geometry = Geometry.Builder().vertices(vertices).indices(indices).build(engine)
    return GeometryNode(engine, geometry, material).apply {
        isTouchable = true
        name = "Golghar • Patna"
    }
}
