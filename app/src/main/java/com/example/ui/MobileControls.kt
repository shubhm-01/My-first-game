package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.interaction.RaycastHit
import com.example.player.PlayerState
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Mobile control overlay providing:
 * - Left thumbstick: Virtual joystick for walking and strafing.
 * - Right swipe surface: Smooth camera pitch and yaw rotation.
 * - Center reticle: Interaction crosshair and contextual prompt.
 * - Minimal action button: Contextual trigger for targeted interactables.
 * - Collapsible debug HUD: FPS, coordinates, rotation, raycast hit.
 */
@Composable
fun MobileControlsOverlay(
  playerState: PlayerState,
  fps: Int,
  targetedHit: RaycastHit?,
  statusMessage: String?,
  loadedAssetCount: Int,
  onMovement: (x: Float, y: Float) -> Unit,
  onLookDelta: (deltaX: Float, deltaY: Float) -> Unit,
  onInteract: () -> Unit,
  discoveredEvidenceCount: Int = 0,
  unlockedFactCount: Int = 0,
  onOpenJournal: () -> Unit = {}
) {
  var showDebugHUD by remember { mutableStateOf(false) }

  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
  ) {
    val screenWidth = maxWidth
    val screenHeight = maxHeight

    // 1. Right-side touch surface for camera look / swipe
    // Strictly isolates the left 40% of the landscape screen for the movement joystick
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = screenWidth * 0.40f)
        .pointerInput(Unit) {
          detectDragGestures { change, dragAmount ->
            change.consume()
            onLookDelta(dragAmount.x, dragAmount.y)
          }
        }
    )

    // 2. Left-side Virtual Movement Joystick (comfortably reachable by left thumb in landscape)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = 28.dp, bottom = 18.dp),
      contentAlignment = Alignment.BottomStart
    ) {
      VirtualJoystick(
        onMovement = onMovement
      )
    }

    // 3. Center-screen RED Reticle & Interaction Indicator (strictly centered on camera forward vector)
    Box(
      modifier = Modifier.fillMaxSize(),
      contentAlignment = Alignment.Center
    ) {
      InteractionReticle(
        isTargeted = targetedHit != null,
        prompt = targetedHit?.prompt
      )
    }

    // 4. Action Button when an interactable object is targeted (comfortably reachable by right thumb in landscape)
    AnimatedVisibility(
      visible = targetedHit != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(end = 36.dp, bottom = 22.dp)
    ) {
      Button(
        onClick = onInteract,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xFFD4A359), // Alchemical gold container
          contentColor = Color(0xFF140F07)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        modifier = Modifier
          .testTag("interact_button")
          .height(50.dp)
          .widthIn(min = 130.dp)
      ) {
        Text(
          text = targetedHit?.actionVerb?.label ?: targetedHit?.interactable?.primaryAction?.label ?: "INTERACT",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          letterSpacing = 1.sp
        )
      }
    }

    // 5. Ephemeral status banner / dialogue feedback (compact top-center in landscape)
    AnimatedVisibility(
      visible = statusMessage != null,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 42.dp, start = 32.dp, end = 32.dp)
    ) {
      Surface(
        color = Color(0xDD121018),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A3E59)),
        modifier = Modifier.widthIn(max = 440.dp)
      ) {
        Text(
          text = statusMessage ?: "",
          color = Color(0xFFE8E0D2),
          fontSize = 13.sp,
          lineHeight = 18.sp,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
      }
    }

    // 6. Top Bar: App Title & Debug Toggle (compact landscape header)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 8.dp, start = 24.dp, end = 24.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "THE LAST ALCHEMIST",
          color = Color(0xFFE2C48D),
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 2.sp
        )
        Text(
          text = "Chapter 0: Academy Atrium (Landscape 3D)",
          color = Color(0xFFA59E92),
          fontSize = 10.sp
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      Button(
        onClick = onOpenJournal,
        colors = ButtonDefaults.buttonColors(
          containerColor = if (discoveredEvidenceCount > 0) Color(0xFF3C2F4D) else Color(0x882A2433),
          contentColor = Color(0xFFE2C48D)
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF53456B)),
        modifier = Modifier
          .testTag("open_journal_button")
          .height(30.dp)
      ) {
        Text(
          text = if (discoveredEvidenceCount > 0) "JOURNAL ($discoveredEvidenceCount)" else "JOURNAL",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      Button(
        onClick = { showDebugHUD = !showDebugHUD },
        colors = ButtonDefaults.buttonColors(
          containerColor = if (showDebugHUD) Color(0xFF553C7B) else Color(0x882A2433),
          contentColor = Color(0xFFEDE8DF)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
          .testTag("debug_toggle_button")
          .height(30.dp)
      ) {
        Text(
          text = if (showDebugHUD) "HUD ON" else "DEBUG",
          fontSize = 10.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }

    // 7. Collapsible Debug HUD Panel (compact top-right in landscape)
    AnimatedVisibility(
      visible = showDebugHUD,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(top = 44.dp, end = 24.dp)
    ) {
      Surface(
        color = Color(0xEE16131F),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF433857)),
        modifier = Modifier.widthIn(max = 240.dp)
      ) {
        Column(
          modifier = Modifier.padding(10.dp)
        ) {
          Text(
            text = "3D LANDSCAPE DIAGNOSTICS",
            color = Color(0xFFD4A359),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.height(3.dp))
          DebugRow("FPS", "$fps fps")
          DebugRow("Pos X, Z", "%.2f, %.2f".format(playerState.position.x, playerState.position.z))
          DebugRow("Eye Level Y", "%.2f m".format(playerState.position.y))
          DebugRow("Look Yaw", "%.1f°".format(playerState.rotation.yaw))
          DebugRow("Look Pitch", "%.1f°".format(playerState.rotation.pitch))
          DebugRow("Collision", if (playerState.isMoving) "Active (Moving)" else "Stable (Idle)")
          DebugRow("Raycast Hit", targetedHit?.interactable?.entityId ?: "None")
          DebugRow("GLB Assets", "$loadedAssetCount Loaded")
        }
      }
    }
  }
}

@Composable
private fun DebugRow(label: String, value: String) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
  ) {
    Text(
      text = "$label:",
      color = Color(0xFF8B8599),
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace,
      modifier = Modifier.width(90.dp)
    )
    Text(
      text = value,
      color = Color(0xFFD9D4E2),
      fontSize = 10.sp,
      fontFamily = FontFamily.Monospace
    )
  }
}

/**
 * Center-screen red reticle that reacts visually when an interactable object is within range.
 * Configured with a high-contrast dual contour (vibrant red with dark backing) so it remains
 * unmistakably visible against both dark environments (shadows/dungeons) and bright environments
 * (torches/light stone), while staying small, centered, and unobtrusive.
 */
@Composable
fun InteractionReticle(
  isTargeted: Boolean,
  prompt: String?
) {
  val redColor = if (isTargeted) Color(0xFFFF1744) else Color(0xFFFF2A2A)
  val darkBorderColor = Color(0xDD000000)

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.testTag("center_reticle")
  ) {
    Box(
      modifier = Modifier
        .size(if (isTargeted) 22.dp else 14.dp)
        // Outer high-contrast dark contour for bright environments
        .border(
          width = if (isTargeted) 2.dp else 1.5.dp,
          color = darkBorderColor,
          shape = CircleShape
        )
        .padding(0.75.dp)
        // Vivid red ring for dark environments and immediate red identification
        .border(
          width = if (isTargeted) 1.5.dp else 1.2.dp,
          color = redColor,
          shape = CircleShape
        ),
      contentAlignment = Alignment.Center
    ) {
      // Center red pip with dark outline
      Box(
        modifier = Modifier
          .size(if (isTargeted) 5.dp else 3.5.dp)
          .border(0.75.dp, darkBorderColor, CircleShape)
          .background(redColor, shape = CircleShape)
      )
    }

    if (isTargeted && prompt != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        color = Color(0xEE160F12),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF2A2A).copy(alpha = 0.85f))
      ) {
        Text(
          text = prompt,
          color = Color(0xFFFFECEC),
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = 0.5.sp,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }
    }
  }
}

/**
 * Touch virtual joystick with thumb indicator and drag range clamping.
 */
@Composable
fun VirtualJoystick(
  onMovement: (x: Float, y: Float) -> Unit,
  modifier: Modifier = Modifier
) {
  val baseRadiusPx = 130f
  val thumbRadiusPx = 55f
  var thumbOffsetX by remember { mutableFloatStateOf(0f) }
  var thumbOffsetY by remember { mutableFloatStateOf(0f) }

  Box(
    modifier = modifier
      .size(120.dp)
      .testTag("virtual_joystick")
      .background(
        Brush.radialGradient(
          listOf(Color(0x333F3553), Color(0x111E1A29))
        ),
        shape = CircleShape
      )
      .border(1.5.dp, Color(0x55A695B8), CircleShape)
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { offset ->
            val center = Offset(size.width / 2f, size.height / 2f)
            val diff = offset - center
            val dist = diff.getDistance()
            val maxR = size.width / 2f - 20f
            val clamped = if (dist > maxR && dist > 0f) diff * (maxR / dist) else diff
            thumbOffsetX = clamped.x
            thumbOffsetY = clamped.y
            val normX = (clamped.x / maxR).coerceIn(-1f, 1f)
            val normY = (-clamped.y / maxR).coerceIn(-1f, 1f)
            onMovement(normX, normY)
          },
          onDragEnd = {
            thumbOffsetX = 0f
            thumbOffsetY = 0f
            onMovement(0f, 0f)
          },
          onDragCancel = {
            thumbOffsetX = 0f
            thumbOffsetY = 0f
            onMovement(0f, 0f)
          },
          onDrag = { change, dragAmount ->
            change.consume()
            val nextX = thumbOffsetX + dragAmount.x
            val nextY = thumbOffsetY + dragAmount.y
            val dist = sqrt(nextX * nextX + nextY * nextY)
            val maxR = size.width / 2f - 20f
            val (clampedX, clampedY) = if (dist > maxR && dist > 0f) {
              Pair(nextX * (maxR / dist), nextY * (maxR / dist))
            } else {
              Pair(nextX, nextY)
            }
            thumbOffsetX = clampedX
            thumbOffsetY = clampedY
            val normX = (clampedX / maxR).coerceIn(-1f, 1f)
            val normY = (-clampedY / maxR).coerceIn(-1f, 1f)
            onMovement(normX, normY)
          }
        )
      },
    contentAlignment = Alignment.Center
  ) {
    // Thumb knob
    Box(
      modifier = Modifier
        .offset { IntOffset(thumbOffsetX.roundToInt(), thumbOffsetY.roundToInt()) }
        .size(46.dp)
        .background(
          Brush.radialGradient(
            listOf(Color(0xFFE2C48D), Color(0xFF9E7C45))
          ),
          shape = CircleShape
        )
        .border(1.dp, Color(0xFFFFF1D6), CircleShape)
    )
  }
}
