package com.example

import com.example.collision.CollisionSystem
import com.example.core.GameEvent
import com.example.interaction.ActionVerb
import com.example.interaction.DoorState
import com.example.interaction.InteractionSystem
import com.example.player.PlayerController
import com.example.player.Position3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DoorwayTraversalTest {

  /**
   * Validates the exact user-requested sequence:
   * APP START -> approach door -> target door -> OPEN -> door opens ->
   * walk forward -> pass completely through doorway -> stand outside ->
   * turn around -> see the open doorway -> walk back inside -> CLOSE door
   */
  @Test
  fun testExactDoorwayTraversalSequence() {
    val collisionSystem = CollisionSystem()
    val playerController = PlayerController(
      collisionSystem = collisionSystem,
      initialPosition = Position3D(0.0f, 1.7f, 2.8f)
    )
    val interactionSystem = InteractionSystem(collisionSystem = collisionSystem)

    // 1. APP START
    val initialPos = playerController.position
    assertEquals(0.0f, initialPos.x, 0.01f)
    assertEquals(2.8f, initialPos.z, 0.01f)
    assertEquals(DoorState.CLOSED, interactionSystem.door.state)
    assertFalse("Doorway should not be marked open initially", collisionSystem.isDoorOpen)
    assertTrue("Doorway at Z=-5.8 must be blocked when closed", collisionSystem.isColliding(0.0f, -5.8f))

    // 2. APPROACH DOOR
    // Player walks forward towards North (-Z)
    playerController.setMovementInput(0.0f, 1.0f)
    // Advance 45 frames (~1.5 seconds)
    for (i in 0 until 45) {
      playerController.update(1.0f / 30.0f)
    }
    playerController.setMovementInput(0.0f, 0.0f)

    // Verify player approached the door area
    val approachedPos = playerController.position
    assertTrue("Player should have moved forward past Z = 0", approachedPos.z < 0.0f)

    // Move player directly in front of the door (Z = -4.5) to target it
    playerController.reset(spawnPos = Position3D(0.0f, 1.7f, -4.5f), spawnYaw = 0.0f)

    // 3. TARGET DOOR
    val rayDir = playerController.getForwardRayDirection()
    val hit = interactionSystem.performRaycast(playerController.position, rayDir)
    assertNotNull("Raycast should target the door from Z = -4.5", hit)
    assertEquals("door_atrium_gate", hit?.interactable?.entityId)
    assertEquals(ActionVerb.OPEN, hit?.actionVerb)
    assertTrue("Prompt should indicate OPEN", hit?.prompt?.contains("OPEN") == true)

    // 4. OPEN
    val openEvent = hit?.interactable?.onInteract()
    assertTrue("Opening door emits DoorOpened event", openEvent is GameEvent.DoorOpened)
    assertEquals(DoorState.OPEN, interactionSystem.door.state)
    assertTrue("Collision system reflects door open", collisionSystem.isDoorOpen)
    assertFalse("Doorway must no longer be blocked at Z = -5.8", collisionSystem.isColliding(0.0f, -5.8f))

    // 5. WALK FORWARD THROUGH DOORWAY
    playerController.setMovementInput(0.0f, 1.0f) // push forward towards -Z
    for (i in 0 until 60) { // ~2 seconds of walking forward
      playerController.update(1.0f / 30.0f)
    }
    playerController.setMovementInput(0.0f, 0.0f)

    // 6. STAND OUTSIDE
    val outsidePos = playerController.position
    assertTrue(
      "Player must pass completely through doorway beyond Z = -6.2, currently at Z = ${outsidePos.z}",
      outsidePos.z < -6.5f
    )
    assertTrue("Player X should be safely centered on landing", outsidePos.x in -1.5f..1.5f)
    // Verify landing safety boundaries prevent falling into void
    assertTrue("Left balustrade must block falling off terrace", collisionSystem.isColliding(-2.3f, outsidePos.z))
    assertTrue("Right balustrade must block falling off terrace", collisionSystem.isColliding(2.3f, outsidePos.z))

    // 7. TURN AROUND
    // Rotate 180 degrees to face back towards the academy room (+Z)
    playerController.applyLookDelta(180.0f / playerController.lookSensitivity, 0.0f)
    val turnedYaw = playerController.rotation.yaw
    assertTrue("Camera should face ~180 deg (towards +Z), was $turnedYaw", turnedYaw in 175.0f..185.0f)

    // 8. SEE THE OPEN DOORWAY
    val lookBackDir = playerController.getForwardRayDirection()
    assertTrue("Looking back produces positive Z ray direction", lookBackDir.third > 0.9f)
    val outsideDoorHit = interactionSystem.performRaycast(playerController.position, lookBackDir)
    assertNotNull("Player outside should target the open doorway", outsideDoorHit)
    assertEquals("door_atrium_gate", outsideDoorHit?.interactable?.entityId)
    assertEquals(ActionVerb.CLOSE, outsideDoorHit?.actionVerb)
    assertTrue("Prompt from outside should indicate CLOSE", outsideDoorHit?.prompt?.contains("CLOSE") == true)

    // 9. WALK BACK INSIDE
    // With yaw = 180, forward movement walks towards +Z (into the room)
    playerController.setMovementInput(0.0f, 1.0f)
    for (i in 0 until 60) {
      playerController.update(1.0f / 30.0f)
    }
    playerController.setMovementInput(0.0f, 0.0f)

    val insidePos = playerController.position
    assertTrue(
      "Player must walk back inside the room past Z = -5.5, currently at Z = ${insidePos.z}",
      insidePos.z > -5.5f
    )

    // 10. CLOSE DOOR
    // Turn back to face North (-Z) towards the door to close it
    playerController.applyLookDelta(180.0f / playerController.lookSensitivity, 0.0f)
    val closeRayDir = playerController.getForwardRayDirection()
    val closeHit = interactionSystem.performRaycast(playerController.position, closeRayDir)
    assertNotNull("Player inside should target the open doorway to close it", closeHit)
    assertEquals(ActionVerb.CLOSE, closeHit?.actionVerb)

    val closeEvent = closeHit?.interactable?.onInteract()
    assertTrue("Closing door emits DoorClosed event", closeEvent is GameEvent.DoorClosed)
    assertEquals(DoorState.CLOSED, interactionSystem.door.state)
    assertFalse("Collision system reflects door closed", collisionSystem.isDoorOpen)
    assertTrue("Doorway at Z = -5.8 must be blocked again after closing", collisionSystem.isColliding(0.0f, -5.8f))
  }
}
