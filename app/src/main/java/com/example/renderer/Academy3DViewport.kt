package com.example.renderer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import com.google.android.filament.Skybox
import dev.romainguy.kotlin.math.Float3
import dev.romainguy.kotlin.math.Float4
import io.github.sceneview.Scene
import io.github.sceneview.environment.Environment
import io.github.sceneview.math.Position
import io.github.sceneview.math.Rotation
import io.github.sceneview.node.CameraNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberCameraNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironment
import io.github.sceneview.rememberMainLightNode
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes

/**
 * Encapsulates the active Filament 3D scene nodes and camera references for the test room.
 */
class AcademySceneHolder(
  val cameraNode: CameraNode
) {
  var loadedModelCount: Int = 0
    private set

  var doorNode: ModelNode? = null
  var collectibleNode: ModelNode? = null
  private var isDoorOpen: Boolean = false

  fun updateCamera(x: Float, y: Float, z: Float, pitch: Float, yaw: Float) {
    cameraNode.position = Position(x, y, z)
    // SceneView uses pitch on X axis and yaw on Y axis
    cameraNode.rotation = Rotation(x = pitch, y = yaw, z = 0.0f)
  }

  /**
   * Dynamically configures the landscape camera projection and aspect ratio
   * to ensure zero stretching and natural perspective in landscape orientation.
   */
  fun updateProjection(width: Int, height: Int) {
    if (width > 0 && height > 0) {
      val aspect = width.toDouble() / height.toDouble()
      try {
        // 65.0 degrees vertical FOV in landscape provides ~100 degrees horizontal FOV without fish-eye
        cameraNode.camera.setLensProjection(65.0, aspect, 0.05, 100.0)
        cameraNode.camera.setExposure(4.0f, 1.0f / 45.0f, 100.0f)
      } catch (ignored: Throwable) {}
    }
  }

  fun setLoadedCount(count: Int) {
    loadedModelCount = count
  }

  /**
   * Sets door visual state: when open, slides upward into archway ceiling.
   */
  fun setDoorOpen(open: Boolean) {
    isDoorOpen = open
    doorNode?.let { node ->
      // In closed state, door sits in archway at Y = 0.0. In open state, slides up to Y = 3.2m
      val targetY = if (open) 3.2f else 0.0f
      node.position = Position(0.0f, targetY, -5.9f)
    }
  }

  /**
   * Sets collectible visual visibility: when taken, moves below ground seamlessly.
   */
  fun setCollectibleVisible(visible: Boolean) {
    collectibleNode?.let { node ->
      val targetY = if (visible) 0.91f else -100.0f
      node.position = Position(0.7f, targetY, -3.2f)
    }
  }
}

/**
 * 3D Viewport composable hosting the Google Filament rendering surface,
 * calibrated physically-based lighting, environment geometry, and interactive academy props.
 */
@Composable
fun Academy3DViewport(
  modifier: Modifier = Modifier,
  onSceneReady: (AcademySceneHolder) -> Unit
) {
  val engine = rememberEngine()
  val modelLoader = rememberModelLoader(engine = engine)

  // Atmospheric dark dungeon skybox prevents blinding pure-white clear background
  val environment = rememberEnvironment(engine = engine) {
    Environment(
      skybox = Skybox.Builder()
        .color(0.04f, 0.035f, 0.055f, 1.0f)
        .build(engine)
    )
  }

  // First-person camera with EV 9.5 exposure calibrated to candle/torch lit medieval stone halls
  val cameraNode = rememberCameraNode(engine = engine).apply {
    position = Position(0.0f, 1.7f, 2.8f)
    rotation = Rotation(0.0f, 0.0f, 0.0f)
    camera.setLensProjection(65.0, 16.0 / 9.0, 0.05, 100.0)
    camera.setExposure(4.0f, 1.0f / 45.0f, 100.0f)
  }

  // Warm amber directional key light (14,000 lux) angled naturally from the upper vaulted ceiling
  val mainLightNode = rememberMainLightNode(engine = engine).apply {
    intensity = 14_000.0f
    color = Float4(1.0f, 0.92f, 0.82f, 1.0f)
    lightDirection = Float3(0.25f, -0.85f, -0.45f)
  }

  val holder = remember(cameraNode) {
    AcademySceneHolder(cameraNode = cameraNode)
  }

  // Load all 3D GLB assets from Android assets/models/
  val roomModel = remember(modelLoader) {
    modelLoader.createModelInstance(assetFileLocation = "models/academy_room.glb")
  }
  val doorModel = remember(modelLoader) {
    modelLoader.createModelInstance(assetFileLocation = "models/academy_door.glb")
  }
  val tableModel = remember(modelLoader) {
    modelLoader.createModelInstance(assetFileLocation = "models/alchemy_table.glb")
  }
  val herbModel = remember(modelLoader) {
    modelLoader.createModelInstance(assetFileLocation = "models/silver_herb.glb")
  }
  val cauldronModel = remember(modelLoader) {
    modelLoader.createModelInstance(assetFileLocation = "models/ancient_cauldron.glb")
  }
  val crystalModel = remember(modelLoader) {
    modelLoader.createModelInstance(assetFileLocation = "models/mystic_crystal.glb")
  }
  val torchModel = remember(modelLoader) {
    modelLoader.createModelInstance(assetFileLocation = "models/wall_torch.glb")
  }

  val childNodes = rememberNodes {
    var count = 0

    // 1. Academy Room Shell (Walls, vaulted ceiling, pillars, archways, floor)
    if (roomModel != null) {
      add(
        ModelNode(modelInstance = roomModel).apply {
          position = Position(0.0f, 0.0f, 0.0f)
        }
      )
      count++
    }

    // 2. Sanctum Door (Interactable doorway barrier at Z = -5.9m)
    if (doorModel != null) {
      val node = ModelNode(modelInstance = doorModel).apply {
        position = Position(0.0f, 0.0f, -5.9f)
      }
      add(node)
      holder.doorNode = node
      count++
    }

    // 3. Alchemy Workbench (Interactable examination station at Z = -3.5m)
    if (tableModel != null) {
      add(
        ModelNode(modelInstance = tableModel).apply {
          position = Position(0.0f, 0.0f, -3.5f)
        }
      )
      count++
    }

    // 4. Silver Herb Cutting (Interactable collectible item resting on workbench)
    if (herbModel != null) {
      val node = ModelNode(modelInstance = herbModel).apply {
        position = Position(0.7f, 0.91f, -3.2f)
      }
      add(node)
      holder.collectibleNode = node
      count++
    }

    // 5. Ancient Bronze Cauldron
    if (cauldronModel != null) {
      add(
        ModelNode(modelInstance = cauldronModel).apply {
          position = Position(-2.2f, 0.0f, -0.5f)
        }
      )
      count++
    }

    // 6. Mystic Crystal Relic on Obsidian Pedestal
    if (crystalModel != null) {
      add(
        ModelNode(modelInstance = crystalModel).apply {
          position = Position(2.2f, 0.0f, -0.5f)
        }
      )
      count++
    }

    // 7. Wall Torches on West and East stone walls
    if (torchModel != null) {
      add(
        ModelNode(modelInstance = torchModel).apply {
          position = Position(-4.8f, 2.0f, -1.0f)
        }
      )
      add(
        ModelNode(modelInstance = torchModel).apply {
          position = Position(4.8f, 2.0f, -1.0f)
          rotation = Rotation(0.0f, 180.0f, 0.0f)
        }
      )
      count++
    }

    holder.setLoadedCount(count)
    onSceneReady(holder)
  }

  Scene(
    modifier = modifier
      .fillMaxSize()
      .onSizeChanged { size ->
        holder.updateProjection(size.width, size.height)
      },
    engine = engine,
    modelLoader = modelLoader,
    cameraNode = cameraNode,
    environment = environment,
    mainLightNode = mainLightNode,
    cameraManipulator = null, // Custom first-person joystick + swipe look arbitration
    childNodes = childNodes
  )
}
