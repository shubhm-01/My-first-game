package com.example.story

import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.data.SaveableEntity

/**
 * Central story state controller tracking narrative progression, chapter milestones,
 * memory fragment states, discovered areas, and high-level story flags.
 * Completely decoupled from UI and rendering.
 */
class StoryProgression(
  initialChapter: StoryChapter = StoryChapter.CHAPTER_0_ATRIUM,
  override val entityId: String = "story_progression"
) : SaveableEntity {

  var currentChapter: StoryChapter = initialChapter
    private set

  var currentLocation: StoryLocation? = null
    private set

  val chapterIndex: Int get() = currentChapter.ordinal

  private val _discoveredLocations = mutableSetOf<String>()
  val discoveredLocations: Set<String> get() = _discoveredLocations.toSet()

  private val _unlockedFacts = mutableSetOf<String>()
  val unlockedFacts: Set<String> get() = _unlockedFacts.toSet()

  private val _discoveredEvidence = mutableSetOf<String>()
  val discoveredEvidence: Set<String> get() = _discoveredEvidence.toSet()

  private val _triggeredMemories = mutableSetOf<String>()
  val triggeredMemories: Set<String> get() = _triggeredMemories.toSet()

  private val _completedScenarios = mutableSetOf<String>()
  val completedScenarios: Set<String> get() = _completedScenarios.toSet()

  private val _storyFlags = mutableMapOf<String, Boolean>()
  val storyFlags: Map<String, Boolean> get() = _storyFlags.toMap()

  // Standard Phase 5 Story Flags
  companion object {
    const val FLAG_RESTRICTED_WARD_SOLVED = "flag_restricted_ward_solved"
    const val FLAG_RESTRICTED_WING_ENTERED = "flag_restricted_wing_entered"
    const val FLAG_RESEARCH_BAY_DISCOVERED = "flag_research_bay_discovered"
    const val FLAG_ARCHIVE_DISCOVERED = "flag_archive_discovered"
    const val FLAG_FIRST_MEMORY_EXPERIENCED = "flag_first_memory_experienced"
    const val FLAG_DELIBERATE_SEAL_ESTABLISHED = "flag_deliberate_seal_established"
    const val FLAG_AETHER_RELIC_EXAMINED = "flag_aether_relic_examined"
    const val FLAG_DEEP_GATE_INSPECTED = "flag_deep_gate_inspected"
  }

  fun setFlag(flag: String, value: Boolean, eventBus: GameEventBus? = null): Boolean {
    val previous = _storyFlags[flag] ?: false
    if (previous != value) {
      _storyFlags[flag] = value
      eventBus?.emit(GameEvent.StoryFlagChanged(flag, value))
      evaluateChapterAdvancement(eventBus)
      return true
    }
    return false
  }

  fun getFlag(flag: String): Boolean = _storyFlags[flag] ?: false

  fun onPlayerMove(x: Float, z: Float, eventBus: GameEventBus? = null): StoryLocation? {
    val loc = StoryLocation.fromPosition(x, z)
    if (loc != null) {
      currentLocation = loc
      recordLocationEntered(loc.id, eventBus)
    }
    return loc
  }

  fun advanceChapterIfReady(
    evidenceCount: Int,
    unlockedFacts: List<String>,
    eventBus: GameEventBus? = null
  ): Boolean {
    when (currentChapter) {
      StoryChapter.CHAPTER_0_ATRIUM -> {
        if (getFlag("flag_first_puzzle_solved") ||
          getFlag(FLAG_RESTRICTED_WARD_SOLVED) ||
          unlockedFacts.contains("FACT_RESTRICTED_WING_SEALED")
        ) {
          advanceChapter(StoryChapter.CHAPTER_1_RESTRICTED_WING, eventBus)
          return true
        }
      }
      StoryChapter.CHAPTER_1_RESTRICTED_WING -> {
        if (getFlag(FLAG_FIRST_MEMORY_EXPERIENCED) && getFlag(FLAG_DELIBERATE_SEAL_ESTABLISHED)) {
          advanceChapter(StoryChapter.CHAPTER_2_ARCHIVES, eventBus)
          return true
        }
      }
      else -> Unit
    }
    return false
  }

  fun recordLocationEntered(locationId: String, eventBus: GameEventBus? = null): Boolean {
    if (_discoveredLocations.add(locationId)) {
      val locName = StoryLocation.ACADEMY_LOCATIONS.find { it.id == locationId }?.displayName ?: locationId
      eventBus?.emit(GameEvent.LocationDiscovered(locationId, locName))

      // Update story flags based on location
      when (locationId) {
        StoryLocation.LOCATION_RESTRICTED_CORRIDOR -> setFlag(FLAG_RESTRICTED_WING_ENTERED, true, eventBus)
        StoryLocation.LOCATION_RESEARCH_BAY -> setFlag(FLAG_RESEARCH_BAY_DISCOVERED, true, eventBus)
        StoryLocation.LOCATION_RESTRICTED_ARCHIVE -> setFlag(FLAG_ARCHIVE_DISCOVERED, true, eventBus)
        StoryLocation.LOCATION_DEEP_GATE -> setFlag(FLAG_DEEP_GATE_INSPECTED, true, eventBus)
      }
      return true
    }
    return false
  }

  fun recordFactUnlocked(factId: String, eventBus: GameEventBus? = null): Boolean {
    if (_unlockedFacts.add(factId)) {
      if (factId == "FACT_ACADEMY_DELIBERATELY_SEALED" || factId == "FACT_RESTRICTED_WING_SEALED") {
        setFlag(FLAG_DELIBERATE_SEAL_ESTABLISHED, true, eventBus)
      }
      evaluateChapterAdvancement(eventBus)
      return true
    }
    return false
  }

  fun recordEvidenceDiscovered(evidenceId: String): Boolean {
    return _discoveredEvidence.add(evidenceId)
  }

  fun recordMemoryTriggered(memoryId: String, eventBus: GameEventBus? = null): Boolean {
    if (_triggeredMemories.add(memoryId)) {
      setFlag(FLAG_FIRST_MEMORY_EXPERIENCED, true, eventBus)
      evaluateChapterAdvancement(eventBus)
      return true
    }
    return false
  }

  fun recordScenarioCompleted(scenarioId: String, eventBus: GameEventBus? = null): Boolean {
    if (_completedScenarios.add(scenarioId)) {
      setFlag(FLAG_RESTRICTED_WARD_SOLVED, true, eventBus)
      evaluateChapterAdvancement(eventBus)
      return true
    }
    return false
  }

  fun advanceChapter(nextChapter: StoryChapter, eventBus: GameEventBus? = null) {
    if (currentChapter != nextChapter) {
      currentChapter = nextChapter
      eventBus?.emit(
        GameEvent.StoryStageAdvanced(
          chapterId = nextChapter.id,
          chapterTitle = nextChapter.title
        )
      )
    }
  }

  private fun evaluateChapterAdvancement(eventBus: GameEventBus? = null) {
    when (currentChapter) {
      StoryChapter.CHAPTER_0_ATRIUM -> {
        if (getFlag(FLAG_RESTRICTED_WARD_SOLVED) && getFlag(FLAG_RESTRICTED_WING_ENTERED)) {
          advanceChapter(StoryChapter.CHAPTER_1_RESTRICTED_WING, eventBus)
        }
      }
      StoryChapter.CHAPTER_1_RESTRICTED_WING -> {
        if (getFlag(FLAG_FIRST_MEMORY_EXPERIENCED) && getFlag(FLAG_DELIBERATE_SEAL_ESTABLISHED)) {
          // Player has discovered the foundational Restricted Wing truth
          // Prepares ground for future Chapter 2
        }
      }
      else -> Unit
    }
  }

  override fun captureState(): Map<String, String> {
    return mapOf(
      "chapter" to currentChapter.id,
      "discoveredLocations" to _discoveredLocations.joinToString(","),
      "unlockedFacts" to _unlockedFacts.joinToString(","),
      "discoveredEvidence" to _discoveredEvidence.joinToString(","),
      "triggeredMemories" to _triggeredMemories.joinToString(","),
      "completedScenarios" to _completedScenarios.joinToString(","),
      "flags" to _storyFlags.entries.joinToString(",") { "${it.key}:${it.value}" }
    )
  }

  override fun restoreState(state: Map<String, String>) {
    state["chapter"]?.let { currentChapter = StoryChapter.fromId(it) }

    _discoveredLocations.clear()
    state["discoveredLocations"]?.split(",")?.filter { it.isNotBlank() }?.let {
      _discoveredLocations.addAll(it)
    }

    _unlockedFacts.clear()
    state["unlockedFacts"]?.split(",")?.filter { it.isNotBlank() }?.let {
      _unlockedFacts.addAll(it)
    }

    _discoveredEvidence.clear()
    state["discoveredEvidence"]?.split(",")?.filter { it.isNotBlank() }?.let {
      _discoveredEvidence.addAll(it)
    }

    _triggeredMemories.clear()
    state["triggeredMemories"]?.split(",")?.filter { it.isNotBlank() }?.let {
      _triggeredMemories.addAll(it)
    }

    _completedScenarios.clear()
    state["completedScenarios"]?.split(",")?.filter { it.isNotBlank() }?.let {
      _completedScenarios.addAll(it)
    }

    _storyFlags.clear()
    state["flags"]?.split(",")?.filter { it.contains(":") }?.forEach { entry ->
      val (key, value) = entry.split(":", limit = 2)
      _storyFlags[key] = value.toBooleanStrictOrNull() ?: false
    }
  }
}
