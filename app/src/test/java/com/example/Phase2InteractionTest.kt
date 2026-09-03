package com.example

import com.example.collision.CollisionSystem
import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.interaction.AcademyDoor
import com.example.interaction.ActionVerb
import com.example.interaction.AlchemyTable
import com.example.interaction.CollectibleIngredient
import com.example.interaction.CollectibleState
import com.example.interaction.DoorState
import com.example.interaction.InteractionSystem
import com.example.player.PlayerController
import com.example.player.Position3D
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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
class Phase2InteractionTest {

  @Test
  fun testAcademyDoorOpenAndCloseLifecycle() {
    val collisionSystem = CollisionSystem()
    val door = AcademyDoor(collisionSystem = collisionSystem)

    // Initial state: CLOSED
    assertEquals(DoorState.CLOSED, door.state)
    assertEquals(ActionVerb.OPEN, door.primaryAction)
    assertTrue(door.actionPrompt.contains("OPEN"))
    // Obstacle should block path at Z = -5.8 when closed
    assertTrue("Should collide with closed door barrier", collisionSystem.isColliding(0.0f, -5.8f))

    // Action 1: OPEN
    val openEvent = door.onInteract()
    assertTrue("Should emit DoorOpened event", openEvent is GameEvent.DoorOpened)
    assertEquals(DoorState.OPEN, door.state)
    assertEquals(ActionVerb.CLOSE, door.primaryAction)
    assertTrue(door.actionPrompt.contains("CLOSE"))
    // Obstacle should be cleared
    assertFalse("Doorway should be clear when open", collisionSystem.isColliding(0.0f, -5.8f))

    // Action 2: CLOSE
    val closeEvent = door.onInteract()
    assertTrue("Should emit DoorClosed event", closeEvent is GameEvent.DoorClosed)
    assertEquals(DoorState.CLOSED, door.state)
    assertEquals(ActionVerb.OPEN, door.primaryAction)
    assertTrue("Should collide again when closed", collisionSystem.isColliding(0.0f, -5.8f))
  }

  @Test
  fun testAcademyDoorStatePersistence() {
    val door = AcademyDoor()
    door.onInteract() // open
    assertEquals(DoorState.OPEN, door.state)

    val savedState = door.captureState()
    assertEquals("OPEN", savedState["state"])

    val newDoorInstance = AcademyDoor()
    assertEquals(DoorState.CLOSED, newDoorInstance.state)
    newDoorInstance.restoreState(savedState)
    assertEquals(DoorState.OPEN, newDoorInstance.state)
  }

  @Test
  fun testAlchemyTableExamination() {
    val table = AlchemyTable()
    assertEquals(ActionVerb.EXAMINE, table.primaryAction)
    assertTrue(table.actionPrompt.contains("EXAMINE"))

    val event = table.onInteract()
    assertTrue(event is GameEvent.ObjectExamined)
    val examined = event as GameEvent.ObjectExamined
    assertEquals(table.entityId, examined.entityId)
    assertTrue(examined.description.contains("alchemy table"))
  }

  @Test
  fun testCollectibleTakeAndStatePersistence() {
    val collectible = CollectibleIngredient()
    assertEquals(CollectibleState.PRESENT, collectible.state)
    assertEquals(ActionVerb.TAKE, collectible.primaryAction)
    assertTrue(collectible.isInteractive)
    assertTrue(collectible.canInteract())

    // Take the item
    val event = collectible.onInteract()
    assertTrue(event is GameEvent.ObjectCollected)
    val collectedEvent = event as GameEvent.ObjectCollected
    assertEquals(collectible.displayName, collectedEvent.itemName)

    // Verify it becomes non-interactive after being taken
    assertEquals(CollectibleState.COLLECTED, collectible.state)
    assertFalse(collectible.isInteractive)
    assertFalse(collectible.canInteract())
    assertNull(collectible.onInteract())

    // State capture & restore
    val state = collectible.captureState()
    assertEquals("COLLECTED", state["state"])

    val restoredItem = CollectibleIngredient()
    assertTrue(restoredItem.canInteract())
    restoredItem.restoreState(state)
    assertFalse(restoredItem.canInteract())
  }

  @Test
  fun testInteractionSystemRaycastTargeting() {
    val collisionSystem = CollisionSystem()
    val interactionSystem = InteractionSystem(collisionSystem = collisionSystem)

    // 1. Raycast targeting the Academy Door (at 0, 1.5, -5.9 from player at 0, 1.7, -4.6 past the table facing North -Z)
    val playerPos = Position3D(0.0f, 1.7f, -4.6f)
    val rayDir = Triple(0.0f, -0.15f, -0.99f)
    val hit = interactionSystem.performRaycast(playerPos, rayDir)

    assertNotNull("Should hit an interactable in front", hit)
    assertEquals("door_atrium_gate", hit?.interactable?.entityId)
    assertEquals(ActionVerb.OPEN, hit?.actionVerb)
    assertTrue(hit?.prompt?.contains("OPEN") == true)

    // Execute interaction through hit
    val event = hit?.interactable?.onInteract()
    assertTrue(event is GameEvent.DoorOpened)

    // Re-raycast after opening: prompt should now be CLOSE
    val reHit = interactionSystem.performRaycast(playerPos, rayDir)
    assertNotNull(reHit)
    assertEquals(ActionVerb.CLOSE, reHit?.actionVerb)
    assertTrue(reHit?.prompt?.contains("CLOSE") == true)
  }

  @Test
  fun testInteractionSystemRaycastTargetingCollectible() {
    val interactionSystem = InteractionSystem()

    // Collectible is at Position3D(0.7f, 0.95f, -3.2f)
    // Aiming directly at the collectible item from player at (0.7f, 1.7f, -2.2f)
    val playerPos = Position3D(0.7f, 1.7f, -2.2f)
    val rayDir = Triple(0.0f, -0.6f, -0.8f) // Looking down-forward directly at item
    val hit = interactionSystem.performRaycast(playerPos, rayDir)

    assertNotNull("Should detect collectible", hit)
    assertEquals("ingredient_silver_leaf", hit?.interactable?.entityId)
    assertEquals(ActionVerb.TAKE, hit?.actionVerb)

    // Take it
    hit?.interactable?.onInteract()

    // Aim away or check that the exact item is no longer returned
    val secondHit = interactionSystem.performRaycast(playerPos, rayDir)
    assertTrue("Item itself is no longer interactable", secondHit?.interactable?.entityId != "ingredient_silver_leaf")
  }

  @Test
  fun testGameEventBusEmission() = runTest {
    val eventBus = GameEventBus()
    var receivedEvent: GameEvent? = null

    val job = launch {
      eventBus.events.collect {
        receivedEvent = it
      }
    }

    eventBus.emit(GameEvent.DoorOpened("door_test"))
    kotlinx.coroutines.yield()

    assertTrue(receivedEvent is GameEvent.DoorOpened)
    assertEquals("door_test", (receivedEvent as GameEvent.DoorOpened).doorId)

    job.cancel()
  }

  @Test
  fun testPlayerMovementAndInteractionRaycastTargeting() {
    val collisionSystem = CollisionSystem()
    val playerController = PlayerController(collisionSystem = collisionSystem)
    val interactionSystem = InteractionSystem(collisionSystem = collisionSystem)

    val door = AcademyDoor(collisionSystem = collisionSystem)
    val table = AlchemyTable()
    val herb = CollectibleIngredient()

    interactionSystem.registerInteractable(door)
    interactionSystem.registerInteractable(table)
    interactionSystem.registerInteractable(herb)

    // Player moves toward the workbench: simulate 2 seconds of forward movement (joystickY = 1.0f)
    playerController.setMovementInput(0.0f, 1.0f)
    for (i in 0 until 60) {
      playerController.update(1.0f / 30.0f)
    }

    val state = playerController.update(0.0f)
    // Player has advanced from Z = 2.8m into the room towards the workbench
    assertTrue("Player should have moved forward", state.position.z < 2.0f)

    // Looking at the table from position (0, 1.7, -2.2)
    playerController.reset(spawnPos = Position3D(0.0f, 1.7f, -2.2f), spawnYaw = 0.0f)
    // Tilt camera slightly down to look at table (-22 degrees pitch)
    playerController.applyLookDelta(0.0f, 22.0f / 0.12f) // swiping down gives negative pitch

    val dir = playerController.getForwardRayDirection()
    assertTrue("Pitching down produces negative Y ray direction", dir.second < 0.0f)

    val hit = interactionSystem.performRaycast(
      rayOrigin = playerController.update(0f).position,
      rayDir = dir
    )
    assertNotNull("Raycast should target the alchemy table or collectible", hit)
    assertTrue(hit?.interactable is AlchemyTable || hit?.interactable is CollectibleIngredient)
  }
}
