package com.example

import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.core.GameMode
import com.example.core.GameState
import com.example.core.WorldState
import com.example.interaction.Interactable
import com.example.interaction.InteractionType
import com.example.magic.AbilityType
import com.example.player.PlayerState
import com.example.player.Position3D
import com.example.player.Rotation2D
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureFoundationTest {

  @Test
  fun testPlayerStateDefaults() {
    val state = PlayerState()
    assertEquals(0.0f, state.position.x, 0.001f)
    assertEquals(1.7f, state.position.y, 0.001f) // eye level
    assertEquals(0.0f, state.position.z, 0.001f)
    assertEquals(0.0f, state.rotation.yaw, 0.001f)
    assertEquals(0.0f, state.rotation.pitch, 0.001f)
  }

  @Test
  fun testWorldStateProgression() {
    val world = WorldState(
      currentRoomId = "grand_library",
      unlockedPassages = setOf("secret_bookshelf"),
      unlockedAbilities = setOf(AbilityType.LIGHT)
    )
    assertEquals("grand_library", world.currentRoomId)
    assertTrue(world.unlockedPassages.contains("secret_bookshelf"))
    assertTrue(world.unlockedAbilities.contains(AbilityType.LIGHT))
  }

  @Test
  fun testEventBusFlow() = runTest {
    val bus = GameEventBus()
    bus.emit(GameEvent.MessageEmitted("The ancient sigil glows."))
    val event = bus.events.first()
    assertTrue(event is GameEvent.MessageEmitted)
    assertEquals("The ancient sigil glows.", (event as GameEvent.MessageEmitted).text)
  }

  @Test
  fun testInteractableContract() {
    val mockLever = object : Interactable {
      override val entityId = "lever_entrance_01"
      override val displayName = "Rusted Iron Lever"
      override val interactionType = InteractionType.SWITCH
      override val actionPrompt = "Pull Lever"
      override val isInteractive = true
      var pulled = false

      override fun onInteract(): GameEvent {
        pulled = !pulled
        return GameEvent.InteractionTriggered(entityId, "PULLED")
      }

      override fun captureState() = mapOf("pulled" to pulled.toString())
      override fun restoreState(state: Map<String, String>) {
        pulled = state["pulled"]?.toBoolean() ?: false
      }
    }

    assertEquals("lever_entrance_01", mockLever.entityId)
    assertEquals("Pull Lever", mockLever.actionPrompt)
    val event = mockLever.onInteract()
    assertNotNull(event)
    val state = mockLever.captureState()
    assertEquals("true", state["pulled"])
  }
}
