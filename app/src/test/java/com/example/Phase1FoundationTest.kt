package com.example

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import com.example.collision.CollisionSystem
import com.example.core.GameEvent
import com.example.interaction.InteractionSystem
import com.example.player.PlayerController
import com.example.player.Position3D
import com.example.player.Rotation2D
import com.example.renderer.AcademySceneHolder
import com.example.renderer.AcademyWorldRenderer
import com.example.renderer.SceneDefinition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Phase1FoundationTest {

  @Test
  fun testLandscapeActivityConfiguration() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pm = context.packageManager
    val activityInfo = pm.getActivityInfo(
      android.content.ComponentName(context, MainActivity::class.java),
      PackageManager.GET_META_DATA
    )
    assertEquals(
      "MainActivity must be configured for sensorLandscape in manifest",
      ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
      activityInfo.screenOrientation
    )
  }

  @Test
  fun testPlayerControllerPitchClamping() {
    val controller = PlayerController()

    // Drag up excessively
    controller.applyLookDelta(deltaX = 0.0f, deltaY = 1000.0f)
    assertTrue("Pitch should clamp to minPitch (-85)", controller.rotation.pitch >= -85.0f)

    // Drag down excessively
    controller.applyLookDelta(deltaX = 0.0f, deltaY = -2000.0f)
    assertTrue("Pitch should clamp to maxPitch (85)", controller.rotation.pitch <= 85.0f)
  }

  @Test
  fun testPlayerControllerMovement() {
    val controller = PlayerController(
      initialPosition = Position3D(0.0f, 1.7f, 0.0f),
      initialRotation = Rotation2D(yaw = 0.0f, pitch = 0.0f)
    )

    // Walk forward (joystickY = 1.0)
    controller.setMovementInput(0.0f, 1.0f)
    val state = controller.update(deltaSeconds = 0.1f)

    assertTrue(state.isMoving)
    // Moving forward with yaw = 0 decreases Z (towards -Z)
    assertTrue("Player should move forward along -Z", state.position.z < 0.0f)
  }

  @Test
  fun testCollisionSystemWallsAndPillars() {
    val collision = CollisionSystem()

    // Center of room should be free
    assertFalse(collision.isColliding(0.0f, 0.0f))

    // Beyond North wall should collide
    assertTrue(collision.isColliding(0.0f, -6.5f))

    // Beyond East wall should collide
    assertTrue(collision.isColliding(5.5f, 0.0f))

    // Inside NW pillar (-2.2, -2.0) should collide
    assertTrue(collision.isColliding(-2.2f, -2.0f))

    // Inside Alchemy Table (0.0, -3.5) should collide
    assertTrue(collision.isColliding(0.0f, -3.5f))
  }

  @Test
  fun testWallSlidingBehavior() {
    val collision = CollisionSystem()

    // Player tries to move diagonally against the East wall (X = 4.65 is boundary)
    val startX = 4.60f
    val startZ = 0.0f

    // Try moving +X (blocked) and +Z (free)
    val (resX, resZ) = collision.resolveMovement(
      currentX = startX,
      currentZ = startZ,
      deltaX = 1.0f, // Blocked by east wall
      deltaZ = 0.5f  // Free to slide along Z
    )

    // X should be clamped/held, while Z successfully slides forward
    assertTrue("X should be constrained by wall", resX <= 4.65f)
    assertTrue("Z should slide smoothly along wall", resZ > startZ)
  }

  @Test
  fun testInteractionRaycastHit() {
    val interaction = InteractionSystem()

    // Player positioned right in front of Cauldron (-2.2, 1.7, 0.8) aiming towards it
    val playerPos = Position3D(-2.2f, 1.7f, 0.8f)
    // Aiming forward along -Z and slightly down
    val rayDir = Triple(0.0f, -0.6f, -0.8f)

    val hit = interaction.performRaycast(playerPos, rayDir)
    assertNotNull("Should detect cauldron in front of camera", hit)
    assertEquals("prop_cauldron", hit?.interactable?.entityId)
    assertTrue(hit?.prompt?.contains("Cauldron") == true)

    val event = hit?.interactable?.onInteract()
    assertTrue(event is GameEvent.InteractionTriggered)
  }

  @Test
  fun testInteractionRaycastMiss() {
    val interaction = InteractionSystem()

    // Player positioned in center aiming at an empty corner
    val playerPos = Position3D(0.0f, 1.7f, 0.0f)
    val rayDir = Triple(1.0f, 0.0f, 0.0f) // Aiming due East away from props

    val hit = interaction.performRaycast(playerPos, rayDir)
    assertNull("Should not hit any interactable when aiming at empty space", hit)
  }

  @Test
  fun testWorldRendererContract() {
    val renderer = AcademyWorldRenderer()
    renderer.initialize()

    val sceneDef = SceneDefinition(
      sceneId = "test_atrium",
      displayName = "Academy Atrium",
      modelAssetPath = "models/academy_room.glb"
    )

    var loaded = false
    renderer.loadScene(sceneDef) {
      loaded = true
    }

    assertTrue(loaded)
    renderer.updateCamera(Position3D(1.0f, 1.7f, 2.0f), Rotation2D(15.0f, -5.0f))
    renderer.destroy()
  }
}
