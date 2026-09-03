package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.audio.AlchemistAudioController
import com.example.audio.AmbientTrack
import com.example.audio.SoundEffect
import com.example.collision.CollisionSystem
import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.interaction.AcademyDoor
import com.example.interaction.AlchemyTable
import com.example.interaction.CollectibleIngredient
import com.example.interaction.InteractionSystem
import com.example.interaction.RaycastHit
import com.example.player.PlayerController
import com.example.renderer.Academy3DViewport
import com.example.renderer.AcademyWorldRenderer
import com.example.renderer.SceneViewProbe
import com.example.ui.MobileControlsOverlay
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Enforce horizontal landscape orientation for gameplay
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    enableEdgeToEdge()

    // Immersive landscape presentation without letterboxing
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.systemBarsBehavior =
      WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())

    setContent {
      MyApplicationTheme {
        Scaffold(
          contentWindowInsets = WindowInsets(0, 0, 0, 0),
          modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold")
        ) { innerPadding ->
          TheLastAlchemistPlayableScreen(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          )
        }
      }
    }
  }
}

/**
 * The primary playable 3D prototype screen for Phase 1 (3D Foundation).
 * Hosts the Google Filament / SceneView viewport, decoupled PlayerController,
 * CollisionSystem, center-screen Raycast, procedural AudioController, and Mobile Controls.
 */
@Composable
fun TheLastAlchemistPlayableScreen(
  modifier: Modifier = Modifier
) {
  val coroutineScope = rememberCoroutineScope()

  // 1. Decoupled subsystems
  val collisionSystem = remember { CollisionSystem() }
  val playerController = remember { PlayerController(collisionSystem = collisionSystem) }
  val interactionSystem = remember { InteractionSystem(collisionSystem = collisionSystem) }
  val doorProp = remember { AcademyDoor(collisionSystem = collisionSystem) }
  val alchemyTableProp = remember { AlchemyTable() }
  val collectibleProp = remember { CollectibleIngredient() }
  val worldRenderer = remember { AcademyWorldRenderer().apply { initialize() } }
  val audioController = remember { AlchemistAudioController() }
  val eventBus = remember { GameEventBus() }

  // Register interactive props into spatial query system
  LaunchedEffect(interactionSystem) {
    interactionSystem.registerInteractable(doorProp)
    interactionSystem.registerInteractable(alchemyTableProp)
    interactionSystem.registerInteractable(collectibleProp)
  }

  // 2. Coarse UI state (never updated at 60 FPS to prevent Compose recomposition thrash)
  var playerState by remember { mutableStateOf(playerController.update(0f)) }
  var targetedHit by remember { mutableStateOf<RaycastHit?>(null) }
  var statusMessage by remember { mutableStateOf<String?>(null) }
  var fps by remember { mutableIntStateOf(60) }
  var showPhase0Arch by remember { mutableStateOf(false) }

  // 3. Event Bus Collector: handles audio feedback, visual updates, and status messages cleanly decoupled
  LaunchedEffect(eventBus) {
    eventBus.events.collect { event ->
      when (event) {
        is GameEvent.ObjectFocused -> {
          audioController.playSfx(SoundEffect.UI_CLICK, 0.15f)
        }
        is GameEvent.DoorOpened -> {
          audioController.playSfx(SoundEffect.DOOR_OPEN, 0.7f)
          worldRenderer.onDoorStateChanged(true)
          statusMessage = "Academy Sanctum Door opened."
        }
        is GameEvent.DoorClosed -> {
          audioController.playSfx(SoundEffect.DOOR_CLOSE, 0.75f)
          worldRenderer.onDoorStateChanged(false)
          statusMessage = "Academy Sanctum Door closed."
        }
        is GameEvent.ObjectCollected -> {
          audioController.playSfx(SoundEffect.ITEM_PICKUP, 0.65f)
          worldRenderer.onCollectibleStateChanged(false)
          statusMessage = "Collected ${event.itemName}."
        }
        is GameEvent.ObjectExamined -> {
          audioController.playSfx(SoundEffect.MYSTERY_CHIME, 0.45f)
          statusMessage = event.description
        }
        is GameEvent.InteractionTriggered -> {
          audioController.playSfx(SoundEffect.MYSTERY_CHIME, 0.4f)
          statusMessage = event.action
        }
        else -> Unit
      }

      // Automatically dismiss status messages after 3.5s
      if (statusMessage != null) {
        launch {
          delay(3500)
          statusMessage = null
        }
      }
    }
  }

  // 4. Audio lifecycle: subterranean atmospheric drone
  DisposableEffect(Unit) {
    audioController.playAmbience(AmbientTrack.SUBTERRANEAN_ECHO)
    onDispose {
      audioController.stopAmbience()
      worldRenderer.destroy()
    }
  }

  // 5. Real-time game loop: advances player locomotion, wall sliding, camera sync, and raycasting
  LaunchedEffect(Unit) {
    var lastNanos = 0L
    var frameCount = 0
    var fpsTimer = 0.0f
    var footstepTimer = 0.0f
    var lastFocusedId: String? = null

    while (isActive) {
      withFrameNanos { now ->
        val dt = if (lastNanos == 0L) 0.016f else ((now - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.1f)
        lastNanos = now

        // 5a. Update player physics and collision
        val currentPState = playerController.update(dt)

        // 5b. Direct GPU camera transform update (zero Compose recomposition)
        worldRenderer.updateCamera(
          position = currentPState.position,
          rotation = currentPState.rotation
        )

        // 5c. Footstep cadence when moving
        if (currentPState.isMoving) {
          footstepTimer += dt
          if (footstepTimer >= 0.45f) {
            footstepTimer = 0.0f
            audioController.playSfx(SoundEffect.FOOTSTEP_STONE, 0.28f)
          }
        } else {
          footstepTimer = 0.35f
        }

        // 5d. Center-screen raycasting
        val rayDir = playerController.getForwardRayDirection()
        val hit = interactionSystem.performRaycast(
          rayOrigin = currentPState.position,
          rayDir = rayDir
        )

        val currentTargetId = hit?.interactable?.entityId
        if (currentTargetId != lastFocusedId) {
          lastFocusedId = currentTargetId
          targetedHit = hit
          if (hit != null) {
            eventBus.emit(GameEvent.ObjectFocused(hit.interactable.entityId, hit.prompt))
          } else {
            eventBus.emit(GameEvent.ObjectUnfocused)
          }
        } else if (hit != null && (hit.prompt != targetedHit?.prompt || hit.actionVerb != targetedHit?.actionVerb)) {
          // Update prompt if object internal state changed (e.g. OPEN -> CLOSE)
          targetedHit = hit
        }

        // 5e. Throttled UI diagnostic updates (twice per second)
        frameCount++
        fpsTimer += dt
        if (fpsTimer >= 0.5f) {
          fps = (frameCount / fpsTimer).roundToInt()
          frameCount = 0
          fpsTimer = 0.0f
          playerState = currentPState
        }
      }
    }
  }

  // 6. Interaction trigger action
  val handleInteraction: () -> Unit = {
    val hit = targetedHit
    if (hit != null && hit.interactable.canInteract()) {
      val event = hit.interactable.onInteract()
      if (event != null) {
        eventBus.emit(event)
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    // 1. 3D Viewport
    Academy3DViewport(
      onSceneReady = { holder ->
        worldRenderer.attachSceneHolder(holder)
      }
    )

    // 2. HUD & Mobile Touch Controls Overlay
    MobileControlsOverlay(
      playerState = playerState,
      fps = fps,
      targetedHit = targetedHit,
      statusMessage = statusMessage,
      loadedAssetCount = worldRenderer.getLoadedAssetCount(),
      onMovement = { x, y ->
        playerController.setMovementInput(x, y)
      },
      onLookDelta = { deltaX, deltaY ->
        playerController.applyLookDelta(deltaX, deltaY)
      },
      onInteract = handleInteraction
    )

    // 3. Phase 0 Architecture Inspection Overlay (can be toggled if requested)
    AnimatedVisibility(
      visible = showPhase0Arch,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize()
    ) {
      Box(modifier = Modifier.fillMaxSize()) {
        Phase0Dashboard(modifier = Modifier.fillMaxSize())
        Button(
          onClick = { showPhase0Arch = false },
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(16.dp),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
        ) {
          Text("Return to 3D")
        }
      }
    }
  }
}


data class ArchitectureModule(val name: String, val description: String, val status: String = "Ready")

@Composable
fun Phase0Dashboard(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val isSceneViewSupported = remember { SceneViewProbe().canInstantiate(context) }

  val modules = listOf(
    ArchitectureModule("core", "GameState, WorldState, GameEventBus"),
    ArchitectureModule("renderer", "WorldRenderer abstraction + SceneView/Filament 3D runtime"),
    ArchitectureModule("player", "PlayerState, Position3D, Rotation2D camera look model"),
    ArchitectureModule("interaction", "Interactable contract, SaveableEntity, InteractionType"),
    ArchitectureModule("puzzle", "PuzzleNode lifecycle (LOCKED, ACTIVE, SOLVED)"),
    ArchitectureModule("inventory", "ItemDefinition, ItemCategory, InventoryHolder contract"),
    ArchitectureModule("alchemy", "ElementAspect, AlchemyIngredient, PotionRecipe, AlchemyStation"),
    ArchitectureModule("magic", "AbilityType (Fire, Frost, Light, Time, Gravity), SpellTarget"),
    ArchitectureModule("story", "JournalEntry, StoryProgressTracker narrative contract"),
    ArchitectureModule("data", "SaveableEntity universal state persistence interface"),
    ArchitectureModule("audio", "AudioController contract (SFX & ambient audio channels)"),
    ArchitectureModule("ui", "GameUiState decoupled from high-frequency game loop")
  )

  Surface(
    modifier = modifier.fillMaxSize().testTag("phase0_dashboard"),
    color = Color(0xFF141218)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      Text(
        text = "THE LAST ALCHEMIST",
        color = Color(0xFFE5DEFF),
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        fontFamily = FontFamily.Serif
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Phase 0 — Project Architecture & Core Setup",
        color = Color(0xFFCBBBEF),
        fontSize = 14.sp
      )

      Spacer(modifier = Modifier.height(12.dp))

      Card(
        modifier = Modifier.fillMaxWidth().testTag("engine_status_card"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (isSceneViewSupported) Color(0xFF1F2B1D) else Color(0xFF2D231E)
        )
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Status",
            tint = if (isSceneViewSupported) Color(0xFF81C784) else Color(0xFFFFB74D),
            modifier = Modifier.size(24.dp)
          )
          Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
              text = if (isSceneViewSupported) "3D Engine: SceneView / Filament Verified" else "3D Engine: Ready for Initialization",
              color = Color.White,
              fontWeight = FontWeight.SemiBold,
              fontSize = 14.sp
            )
            Text(
              text = "Native glTF/GLB PBR pipeline ready for Phase 1 3D Foundation",
              color = Color(0xFFC0C0C0),
              fontSize = 12.sp
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "Architecture Packages (${modules.size})",
        color = Color(0xFFD0BCFF),
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
      )

      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(modules) { module ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF211F26))
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = module.name,
                  color = Color(0xFFE6E1E5),
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                )
                Text(
                  text = module.description,
                  color = Color(0xFFCAC4D0),
                  fontSize = 12.sp
                )
              }
              Box(
                modifier = Modifier
                  .background(Color(0xFF382E47), RoundedCornerShape(4.dp))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = module.status,
                  color = Color(0xFFD0BCFF),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
  MyApplicationTheme { Greeting("Android") }
}

