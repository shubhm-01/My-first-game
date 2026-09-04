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

  // Investigation & Evidence System (Phase 3)
  data class EvidenceDiscovered(val evidenceId: String, val title: String, val type: String) : GameEvent()
  data class FactUnlocked(val factId: String, val title: String) : GameEvent()
  data class JournalUpdated(val category: String, val totalEntries: Int) : GameEvent()
  data class InvestigationProgressChanged(val progressFraction: Float, val discoveredCount: Int) : GameEvent()

  // Variable Investigation & Environmental Puzzle System (Phase 4)
  data class InvestigationScenarioStarted(val scenarioId: String, val title: String) : GameEvent()
  data class EvidencePoolGenerated(val poolId: String, val selectedEvidenceIds: List<String>) : GameEvent()
  data class PuzzleConfigured(val puzzleId: String, val configurationId: String) : GameEvent()
  data class PuzzleSolved(val puzzleId: String) : GameEvent()
  data class PuzzleReset(val puzzleId: String) : GameEvent()
  data class InvestigationScenarioCompleted(val scenarioId: String) : GameEvent()

  // Story Progression & Memory Fragments (Phase 5)
  data class StoryStageAdvanced(val chapterId: String, val chapterTitle: String) : GameEvent()
  data class StoryFlagChanged(val flag: String, val value: Boolean) : GameEvent()
  data class MemoryTriggered(val memoryId: String, val title: String, val sensoryEcho: String) : GameEvent()
  data class MemoryCompleted(val memoryId: String) : GameEvent()
  data class LocationDiscovered(val locationId: String, val locationName: String) : GameEvent()
  data class AetherPresenceDetected(val entityId: String, val phenomenonDescription: String) : GameEvent()
}

/**
 * Lightweight, thread-safe asynchronous event bus for decoupling gameplay systems.
 */
class GameEventBus {
  private val _events = MutableSharedFlow<GameEvent>(replay = 16, extraBufferCapacity = 64)
  val events: SharedFlow<GameEvent> = _events.asSharedFlow()

  fun emit(event: GameEvent) {
    _events.tryEmit(event)
  }
}

