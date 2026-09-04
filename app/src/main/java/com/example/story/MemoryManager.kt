package com.example.story

import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.data.SaveableEntity
import com.example.investigation.InvestigationManager

/**
 * Controller orchestrating Aether memory fragment triggers, playback lifecycle,
 * narrative revelation delivery, and integration with investigation records.
 */
class MemoryManager(
  val storyProgression: StoryProgression? = null,
  val investigationManager: InvestigationManager? = null,
  override val entityId: String = "memory_manager"
) : SaveableEntity {

  private val _registeredMemories = mutableMapOf<String, MemoryFragment>()
  val registeredMemories: Map<String, MemoryFragment> get() = _registeredMemories.toMap()

  var activeMemory: MemoryFragment? = null
    private set

  var activePlaybackElapsed: Float = 0.0f
    private set

  val playbackElapsed: Float get() = activePlaybackElapsed

  init {
    // Register certified memory variants
    VariableMemoryPool.ALL_CERTIFIED_VARIANTS.forEach { registerMemory(it) }
  }

  fun registerDefaultMemories(seed: Long = 0L) {
    VariableMemoryPool.ALL_CERTIFIED_VARIANTS.forEach { registerMemory(it) }
    val chosen = VariableMemoryPool.selectMemoryForSeed(seed)
    registerMemory(chosen.copy(id = "mem_council_mandate"))
  }

  fun registerMemory(fragment: MemoryFragment) {
    _registeredMemories[fragment.id] = fragment
  }

  fun getMemory(memoryId: String): MemoryFragment? = _registeredMemories[memoryId]

  fun getDiscoveredMemories(): List<MemoryFragment> =
    _registeredMemories.values.filter { it.isDiscovered }

  fun isMemoryPlaying(): Boolean = activeMemory != null

  fun isMemoryDiscovered(memoryId: String): Boolean =
    _registeredMemories[memoryId]?.isDiscovered == true

  /**
   * Evaluates object interaction triggers.
   */
  fun checkObjectExaminedTrigger(entityId: String, eventBus: GameEventBus? = null): MemoryFragment? {
    for (fragment in _registeredMemories.values) {
      val condition = fragment.triggerCondition
      if (condition is MemoryTriggerCondition.OnObjectExamined && condition.entityId == entityId) {
        if (canTrigger(fragment)) {
          triggerMemory(fragment.id, eventBus)
          return fragment
        }
      }
    }
    return null
  }

  /**
   * Evaluates evidence discovery triggers.
   */
  fun checkEvidenceDiscoveredTrigger(evidenceId: String, eventBus: GameEventBus? = null): MemoryFragment? {
    for (fragment in _registeredMemories.values) {
      val condition = fragment.triggerCondition
      if (condition is MemoryTriggerCondition.OnEvidenceDiscovered && condition.evidenceId == evidenceId) {
        if (canTrigger(fragment)) {
          triggerMemory(fragment.id, eventBus)
          return fragment
        }
      }
    }
    return null
  }

  fun canTrigger(fragment: MemoryFragment): Boolean {
    if (activeMemory != null) return false
    if (fragment.isTriggered && fragment.replayPolicy == MemoryReplayPolicy.PLAY_ONCE) return false
    return true
  }

  /**
   * Initiates playback of an Aether memory echo.
   */
  fun triggerMemory(memoryId: String, eventBus: GameEventBus? = null): Boolean {
    val fragment = _registeredMemories[memoryId] ?: return false
    if (!canTrigger(fragment)) return false

    fragment.isDiscovered = true
    fragment.isTriggered = true
    fragment.discoveredTimestamp = System.currentTimeMillis()

    activeMemory = fragment
    activePlaybackElapsed = 0.0f

    // Record in story progression
    storyProgression?.recordMemoryTriggered(memoryId, eventBus)

    // Emit domain event
    eventBus?.emit(
      GameEvent.MemoryTriggered(
        memoryId = fragment.id,
        title = fragment.title,
        sensoryEcho = fragment.sensoryEcho
      )
    )

    return true
  }

  /**
   * Advances active memory playback timer.
   * Returns active MemoryFragment if currently playing, or null when not playing / finished.
   */
  fun update(deltaSeconds: Float, eventBus: GameEventBus? = null): MemoryFragment? {
    val current = activeMemory ?: return null
    activePlaybackElapsed += deltaSeconds

    if (activePlaybackElapsed >= current.durationSeconds) {
      completeActiveMemory(eventBus)
      return null
    }
    return current
  }

  /**
   * Conclude active memory alias.
   */
  fun concludeActiveMemory(eventBus: GameEventBus? = null) {
    completeActiveMemory(eventBus)
  }

  /**
   * Completes active memory playback, unlocks associated story facts,
   * updates investigation records, and cleans up active state.
   */
  fun completeActiveMemory(eventBus: GameEventBus? = null) {
    val current = activeMemory ?: return

    // Unlock associated story facts
    current.associatedFactIds.forEach { factId ->
      investigationManager?.unlockFact(factId, eventBus)
      storyProgression?.recordFactUnlocked(factId, eventBus)
    }

    eventBus?.emit(GameEvent.MemoryCompleted(current.id))
    activeMemory = null
    activePlaybackElapsed = 0.0f
  }

  /**
   * Allows replaying an already discovered memory via the Journal without altering gameplay state.
   */
  fun replayMemoryViaJournal(memoryId: String, eventBus: GameEventBus? = null): Boolean {
    val fragment = _registeredMemories[memoryId] ?: return false
    if (!fragment.isDiscovered) return false

    activeMemory = fragment
    activePlaybackElapsed = 0.0f

    eventBus?.emit(
      GameEvent.MemoryTriggered(
        memoryId = fragment.id,
        title = fragment.title,
        sensoryEcho = fragment.sensoryEcho
      )
    )
    return true
  }

  override fun captureState(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    _registeredMemories.forEach { (id, memory) ->
      map["memory_${id}_discovered"] = memory.isDiscovered.toString()
      map["memory_${id}_triggered"] = memory.isTriggered.toString()
      map["memory_${id}_timestamp"] = memory.discoveredTimestamp.toString()
    }
    return map
  }

  override fun restoreState(state: Map<String, String>) {
    _registeredMemories.forEach { (id, memory) ->
      state["memory_${id}_discovered"]?.toBooleanStrictOrNull()?.let { memory.isDiscovered = it }
      state["memory_${id}_triggered"]?.toBooleanStrictOrNull()?.let { memory.isTriggered = it }
      state["memory_${id}_timestamp"]?.toLongOrNull()?.let { memory.discoveredTimestamp = it }
    }
  }
}
