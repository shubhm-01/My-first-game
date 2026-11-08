package com.example

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import io.github.sceneview.math.Size
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val MAN_GLB = "https://grab3d.com/dl/man-rigged/man-rigged.glb"
private const val WOMAN_GLB = "https://grab3d.com/dl/woman-rigged/woman-rigged.glb"
private const val HOUSE_GLB = "https://grab3d.com/dl/modern-house/modern-house.glb"
private const val SUV_GLB = "https://grab3d.com/dl/1990s-suv/1990s-suv.glb"
private const val TREE_GLB = "https://grab3d.com/dl/mangrove-tree/mangrove-tree.glb"
private const val STREETLIGHT_GLB = "https://grab3d.com/dl/street-light/street-light.glb"
private const val BUSSTOP_GLB = "https://grab3d.com/dl/bus-stop-shelter/bus-stop-shelter.glb"

private class PatnaInput {
    var forward = false
    var back = false
    var left = false
    var right = false
    var yaw = 0f
    var interact = false
}

private data class ActorWaypoint(val x: Float, val z: Float, val pause: Float = 0f)

/**
 * A third-person, real-time 3D vertical slice set in a fictional 2047 expansion of Patna. This feature branch is APK-build validated by CI.
 * Characters and city props are real textured GLB assets; only the Golghar landmark and
 * terrain/roads are generated geometry. The original Alchemist game remains in the repository.
 */
@Composable
fun Patna2047Screen(modifier: Modifier = Modifier) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberCameraNode(engine) {
        position = Position(x = -14f, y = 3.4f, z = 29f)
        lookAt(Position(x = -14f, y = 1f, z = 22f))
        far = 360f
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
    var nearHome by remember { mutableStateOf(false) }
    var nearGolghar by remember { mutableStateOf(false) }

    val grass = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF71835C), roughness = 0.96f) }
    val asphalt = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF343A40), roughness = 0.9f) }
    val sidewalk = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFB4AA99), roughness = 0.94f) }
    val water = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF347F9A), metallic = 0.18f, roughness = 0.23f) }
    val ghatStone = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFB78E68), roughness = 0.88f) }
    val golgharMaterial = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFC6A47B), roughness = 0.93f) }
    val darkStone = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF604E40), roughness = 0.9f) }
    val roadPaint = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFFE8DDB7), roughness = 0.7f) }
    val parkGreen = remember(materialLoader) { materialLoader.createColorInstance(Color(0xFF506E48), roughness = 0.96f) }

    LaunchedEffect(Unit) {
        // First render the complete land, roads and river so the player sees the world immediately.
        sceneNodes += PlaneNode(engine, size = Size(x = 260f, y = 260f), normal = Direction(y = 1f), materialInstance = grass)
        sceneNodes += PlaneNode(engine, size = Size(x = 240f, y = 48f), normal = Direction(y = 1f), materialInstance = water, position = Position(x = 0f, y = 0.035f, z = -96f))
        sceneNodes += PlaneNode(engine, size = Size(x = 12f, y = 188f), normal = Direction(y = 1f), materialInstance = asphalt, position = Position(x = 0f, y = 0.07f, z = 10f))
        sceneNodes += PlaneNode(engine, size = Size(x = 174f, y = 10f), normal = Direction(y = 1f), materialInstance = asphalt, position = Position(x = 45f, y = 0.075f, z = 24f))
        sceneNodes += PlaneNode(engine, size = Size(x = 174f, y = 8f), normal = Direction(y = 1f), materialInstance = sidewalk, position = Position(x = 45f, y = 0.09f, z = 33f))
        sceneNodes += PlaneNode(engine, size = Size(x = 174f, y = 8f), normal = Direction(y = 1f), materialInstance = sidewalk, position = Position(x = 45f, y = 0.09f, z = 15f))
        sceneNodes += PlaneNode(engine, size = Size(x = 150f, y = 7f), normal = Direction(y = 1f), materialInstance = sidewalk, position = Position(x = -3f, y = 0.09f, z = -57f))
        sceneNodes += PlaneNode(engine, size = Size(x = 150f, y = 4f), normal = Direction(y = 1f), materialInstance = asphalt, position = Position(x = -3f, y = 0.08f, z = -65f))
        // Dash markings and a broad river crossing.
        for (i in -8..8) {
            sceneNodes += PlaneNode(engine, size = Size(x = 0.22f, y = 3.4f), normal = Direction(y = 1f), materialInstance = roadPaint, position = Position(x = 0f, y = 0.095f, z = i * 10f + 10f))
        }
        sceneNodes += PlaneNode(engine, size = Size(x = 9f, y = 60f), normal = Direction(y = 1f), materialInstance = asphalt, position = Position(x = 0f, y = 0.14f, z = -96f))
        for (i in 0..5) {
            sceneNodes += PlaneNode(engine, size = Size(x = 42f - i * 2f, y = 1.45f), normal = Direction(y = 1f), materialInstance = ghatStone, position = Position(x = -58f + i * 1.1f, y = 0.12f + i * 0.08f, z = -69f - i * 2.1f))
        }
        sceneNodes += PlaneNode(engine, size = Size(x = 60f, y = 28f), normal = Direction(y = 1f), materialInstance = parkGreen, position = Position(x = -25f, y = 0.055f, z = -37f))
        // A recognisable Golghar silhouette made as one tapered, shaded mesh rather than primitives.
        val golghar = createGolghar(engine, golgharMaterial)
        golghar.position = Position(x = 56f, y = 0.1f, z = -28f)
        sceneNodes += golghar
        sceneNodes += PlaneNode(engine, size = Size(x = 1.8f, y = 2.4f), normal = Direction(z = 1f), materialInstance = darkStone, position = Position(x = 56f, y = 1.2f, z = -19.65f))
        loadMessage = "Loading people, homes and city assets…"

        suspend fun addModel(url: String, scale: Float, at: Position, yaw: Float = 0f, walk: Boolean = false): ModelNode? {
            return try {
                val instance = modelLoader.loadModelInstance(url)
                if (instance == null) {
                    loadMessage = "Some city assets are unavailable. Continuing with the 3D city…"
                    null
                } else {
                    ModelNode(instance, autoAnimate = false, scaleToUnits = scale, centerOrigin = Position(x = 0f, y = -1f, z = 0f)).also { node ->
                        node.position = at
                        node.rotation = Rotation(y = yaw)
                        if (walk && node.animationCount > 0) node.playAnimation(0, 0.95f, true)
                        sceneNodes += node
                    }
                }
            } catch (_: Exception) {
                loadMessage = "Network asset skipped; the rest of Patna is still playable."
                null
            }
        }

        // Keep detailed assets sparse for mobile GPU/thermal headroom.
        addModel(HOUSE_GLB, 17f, Position(x = -28f, y = 0f, z = 24f), yaw = 90f)
        addModel(HOUSE_GLB, 14f, Position(x = -52f, y = 0f, z = -4f), yaw = 0f)
        val boy = addModel(MAN_GLB, 1.78f, Position(x = -18f, y = 0.12f, z = 18f), yaw = 180f, walk = true)
        val girl = addModel(WOMAN_GLB, 1.7f, Position(x = -21f, y = 0.12f, z = 20f), yaw = 180f, walk = true)
        addModel(SUV_GLB, 4.7f, Position(x = 14f, y = 0.12f, z = 20f), yaw = 90f)
        addModel(TREE_GLB, 8.5f, Position(x = -43f, y = 0.08f, z = -28f))
        addModel(STREETLIGHT_GLB, 8f, Position(x = -4.5f, y = 0.08f, z = 2f))
        addModel(BUSSTOP_GLB, 3.0f, Position(x = 28f, y = 0.08f, z = 18f), yaw = 90f)

        // Ghat benches and riverfront details.
        sceneNodes += CubeNode(engine, size = Size(x = 4.1f, y = 0.48f, z = 0.58f), materialInstance = darkStone).apply { position = Position(x = -20f, y = 0.52f, z = -43f) }
        sceneNodes += CubeNode(engine, size = Size(x = 0.14f, y = 1.15f, z = 0.14f), materialInstance = darkStone).apply { position = Position(x = -21.7f, y = 0.55f, z = -43f) }
        sceneNodes += CubeNode(engine, size = Size(x = 0.14f, y = 1.15f, z = 0.14f), materialInstance = darkStone).apply { position = Position(x = -18.3f, y = 0.55f, z = -43f) }

        // A few warm street lamps and roadway furniture, kept low-poly and spaced out.
        for (x in listOf(-8f, 8f, 24f, 42f, 60f, 78f)) {
            sceneNodes += CylinderNode(engine, radius = 0.08f, height = 5.8f, materialInstance = darkStone).apply { position = Position(x = x, y = 2.9f, z = 10f) }
            sceneNodes += CubeNode(engine, size = Size(x = 0.8f, y = 0.12f, z = 0.12f), materialInstance = roadPaint).apply { position = Position(x = x + 0.28f, y = 5.68f, z = 10f) }
        }

        if (boy != null && girl != null) {
            dialogue = "Aarav aur Meera ghar se nikal pade. Ganga ghat par aaj mela hai."
            objective = "Walk to the Ganga riverfront. Tap INTERACT near Meera."
            worldReady = true
        } else {
            dialogue = "Patna 2047 is ready. Character models are still loading or offline."
            objective = "Explore the city and riverfront."
            worldReady = true
        }
        loading = false
        loadMessage = "Patna 2047 • Ganga Riverfront"
    }

    // The female character follows a small daily routine: home → market → river promenade → home.
    var playerNode by remember { mutableStateOf<ModelNode?>(null) }
    var npcNode by remember { mutableStateOf<ModelNode?>(null) }
    var nearNpc by remember { mutableStateOf(false) }
    val npcRoute = remember {
        listOf(
            ActorWaypoint(-21f, 20f, 1.2f),
            ActorWaypoint(-8f, 5f, 0.5f),
            ActorWaypoint(23f, -28f, 2.5f),
            ActorWaypoint(-20f, -43f, 5.0f),
            ActorWaypoint(-30f, -5f, 0.8f),
            ActorWaypoint(-21f, 20f, 2.0f)
        )
    }
    // Resolve the two rigged actors after the GLB files finish loading.
    LaunchedEffect(sceneNodes.size, worldReady) {
        val animated = sceneNodes.filterIsInstance<ModelNode>().filter { it.animationCount > 0 }
        playerNode = animated.getOrNull(0)
        npcNode = animated.getOrNull(1)
    }

    LaunchedEffect(worldReady, playerNode, npcNode) {
        val player = playerNode ?: return@LaunchedEffect
        val npc = npcNode ?: return@LaunchedEffect
        if (!worldReady) return@LaunchedEffect
        var previous = 0L
        var px = -18f
        var pz = 18f
        var segment = 0
        var pause = 0f
        var lastNear = false
        var lastNearHome = false
        var lastNearGolghar = false
        var lastMoving: Boolean? = null
        var lastNpcWalking: Boolean? = null
        while (true) {
            withFrameNanos { now ->
                val dt = if (previous == 0L) 0.016f else ((now - previous) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                previous = now

                val dx = (if (input.right) 1f else 0f) - (if (input.left) 1f else 0f)
                val dz = (if (input.back) 1f else 0f) - (if (input.forward) 1f else 0f)
                val length = sqrt(dx * dx + dz * dz).coerceAtLeast(1f)
                val moving = dx != 0f || dz != 0f
                if (lastMoving != moving) {
                    player.setAnimationSpeed(0, if (moving) 0.95f else 0.12f)
                    lastMoving = moving
                }
                if (moving) {
                    val cYaw = cos(input.yaw)
                    val sYaw = sin(input.yaw)
                    px = (px + (dx * cYaw - dz * sYaw) * 3.8f * dt / length).coerceIn(-105f, 105f)
                    pz = (pz + (dx * sYaw + dz * cYaw) * 3.8f * dt / length).coerceIn(-52f, 95f)
                    player.position = Position(x = px, y = 0.12f, z = pz)
                    player.rotation = Rotation(y = input.yaw * 180f / PI.toFloat() + 180f)
                }
                cameraNode.position = Position(
                    x = px + sin(input.yaw) * 6.0f,
                    y = 3.45f,
                    z = pz + cos(input.yaw) * 6.0f
                )
                cameraNode.lookAt(Position(x = px, y = 1.15f, z = pz))

                val target = npcRoute[segment]
                val nx = target.x - npc.position.x
                val nz = target.z - npc.position.z
                val distance = sqrt(nx * nx + nz * nz)
                val npcWalking = distance >= 0.65f
                if (lastNpcWalking != npcWalking) {
                    npc.setAnimationSpeed(0, if (npcWalking) 0.85f else 0.12f)
                    lastNpcWalking = npcWalking
                }
                if (distance < 0.65f) {
                    if (pause < target.pause) pause += dt
                    else {
                        pause = 0f
                        segment = (segment + 1) % npcRoute.size
                    }
                } else {
                    val step = (1.5f * dt).coerceAtMost(distance)
                    npc.position = Position(
                        x = npc.position.x + nx / distance * step,
                        y = 0.12f,
                        z = npc.position.z + nz / distance * step
                    )
                    npc.rotation = Rotation(y = kotlin.math.atan2(nx, nz) * 180f / PI.toFloat() + 180f)
                }

                val nowNear = sqrt(
                    (px - npc.position.x) * (px - npc.position.x) +
                        (pz - npc.position.z) * (pz - npc.position.z)
                ) < 4.3f
                if (nowNear != lastNear) {
                    lastNear = nowNear
                    nearNpc = nowNear
                }
                val homeNearby = sqrt((px + 28f) * (px + 28f) + (pz - 24f) * (pz - 24f)) < 7f
                if (homeNearby != lastNearHome) {
                    lastNearHome = homeNearby
                    nearHome = homeNearby
                }
                val golgharNearby = sqrt((px - 56f) * (px - 56f) + (pz + 28f) * (pz + 28f)) < 11f
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

        // Drag on the open right side to orbit the third-person camera.
        Box(
            Modifier.fillMaxHeight().fillMaxWidth(0.57f).align(Alignment.CenterEnd)
                .pointerInput(Unit) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        input.yaw += drag.x * 0.0045f
                        input.yaw = input.yaw.coerceIn(-1.25f, 1.25f)
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
                Text(if (loading) loadMessage else "AARAV  •  MEERA  •  GOLGHAR", color = Color.White, fontSize = 9.sp)
                Text("Drag right side to look", color = Color(0xFFB4C5C3), fontSize = 9.sp)
            }
        }

        // Left thumb D-pad — press and hold to walk.
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(start = 22.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            HoldDirection("▲", { input.forward = it })
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                HoldDirection("◀", { input.left = it })
                HoldDirection("▼", { input.back = it })
                HoldDirection("▶", { input.right = it })
            }
        }

        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 22.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    when {
                        nearNpc -> {
                            hasMet = true
                            dialogue = "Meera: Aarav, ghat par thodi der rukte hain. Aaj Ganga aarti dekhni hai!  •  Aarav: Haan, phir ghar lautenge."
                            objective = "Story 01 complete: Meet Meera. Explore Golghar next."
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

        if (loading) {
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
private fun HoldDirection(label: String, onHold: (Boolean) -> Unit) {
    Box(
        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(15.dp))
            .background(Color(0xCC18252B))
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onHold(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        onHold(false)
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
