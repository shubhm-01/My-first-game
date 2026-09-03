package com.example.core

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Domain events emitted across systems (interaction, discovery, puzzles, magic).
 */
sealed class GameEvent {
  // Targeting & Focus
  data class ObjectFocused(val targetId: String?, val prompt: String?) : GameEvent()
  data object ObjectUnfocused : GameEvent()

  // General Interaction
  data class InteractionTriggered(val targetId: String, val action: String) : GameEvent()
  data class ObjectInteracted(val targetId: String, val actionVerb: String) : GameEvent()

  // Specific Action Events
  data class DoorOpened(val doorId: String) : GameEvent()
  data class DoorClosed(val doorId: String) : GameEvent()
  data class ObjectCollected(val entityId: String, val itemName: String) : GameEvent()
  data class ObjectExamined(val entityId: String, val description: String) : GameEvent()

  // Future System Reusability
  data class ItemAcquired(val itemId: String, val itemName: String) : GameEvent()
  data class ClueDiscovered(val clueId: String, val title: String) : GameEvent()
  data class PuzzleStateChanged(val puzzleId: String, val isSolved: Boolean) : GameEvent()
  data class AbilityUnlocked(val abilityName: String) : GameEvent()
  data class AreaTransition(val fromAreaId: String, val toAreaId: String) : GameEvent()
  data class MessageEmitted(val text: String) : GameEvent()
}

/**
 * Lightweight, thread-safe asynchronous event bus for decoupling gameplay systems.
 */
class GameEventBus {
  private val _events = MutableSharedFlow<GameEvent>(replay = 1, extraBufferCapacity = 64)
  val events: SharedFlow<GameEvent> = _events.asSharedFlow()

  fun emit(event: GameEvent) {
    _events.tryEmit(event)
  }
}

