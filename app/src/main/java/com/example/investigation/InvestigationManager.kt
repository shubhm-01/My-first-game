package com.example.investigation

import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.data.SaveableEntity

/**
 * Core manager for the game's narrative deduction, evidence collection, and fact reconstruction.
 * Coordinates evidence discovery, multi-clue story fact unlocking, and variable clue solvability.
 */
class InvestigationManager(
  override val entityId: String = "investigation_system"
) : SaveableEntity {

  // Master definitions catalog
  private val _evidenceCatalog = mutableMapOf<String, EvidenceItem>()
  val evidenceCatalog: Map<String, EvidenceItem> get() = _evidenceCatalog

  private val _storyFacts = mutableMapOf<String, StoryFact>()
  val storyFacts: Map<String, StoryFact> get() = _storyFacts

  private val _variablePools = mutableMapOf<String, VariableEvidencePool>()
  val variablePools: Map<String, VariableEvidencePool> get() = _variablePools

  // Dynamic player session state
  private val _discoveredEvidence = mutableMapOf<String, EvidenceItem>()
  private val _unlockedFacts = mutableMapOf<String, StoryFact>()
  private val _examinedObjects = mutableSetOf<String>()

  /**
   * Registers an evidence definition into the catalog.
   */
  fun registerEvidence(item: EvidenceItem) {
    _evidenceCatalog[item.id] = item
  }

  /**
   * Registers a story fact definition into the catalog.
   */
  fun registerFact(fact: StoryFact) {
    _storyFacts[fact.id] = fact
  }

  /**
   * Registers a variable evidence pool configuration.
   */
  fun registerVariablePool(pool: VariableEvidencePool) {
    _variablePools[pool.poolId] = pool
  }

  /**
   * Records that a physical in-world object was inspected by the player.
   */
  fun recordObjectExamined(objectId: String) {
    _examinedObjects.add(objectId)
  }

  fun hasExaminedObject(objectId: String): Boolean {
    return objectId in _examinedObjects
  }

  /**
   * Discovers an evidence item by ID:
   * 1. Validates existence in catalog.
   * 2. Prevents duplicate discovery.
   * 3. Records discovery with timestamp.
   * 4. Evaluates whether any corroborating StoryFacts are now unlocked.
   * 5. Emits corresponding domain events via [eventBus].
   *
   * Returns the discovered [EvidenceItem], or null if already discovered or invalid.
   */
  fun discoverEvidence(evidenceId: String, eventBus: GameEventBus? = null): EvidenceItem? {
    val template = _evidenceCatalog[evidenceId] ?: return null

    // Duplicate prevention: return null if already registered
    if (_discoveredEvidence.containsKey(evidenceId)) {
      return null
    }

    val timestamp = System.currentTimeMillis()
    val discovered = template.copy(
      isDiscovered = true,
      discoveredTimestamp = timestamp
    )
    _discoveredEvidence[evidenceId] = discovered

    // Emit EvidenceDiscovered event
    eventBus?.emit(
      GameEvent.EvidenceDiscovered(
        evidenceId = discovered.id,
        title = discovered.title,
        type = discovered.type.name
      )
    )

    // Evaluate associated StoryFacts
    evaluateStoryFacts(eventBus)

    // Emit JournalUpdated event
    val categoryCount = _discoveredEvidence.values.count { it.category == discovered.category }
    eventBus?.emit(
      GameEvent.JournalUpdated(
        category = discovered.category.name,
        totalEntries = categoryCount
      )
    )

    // Emit progress change event
    val currentProgress = getProgressFraction()
    eventBus?.emit(
      GameEvent.InvestigationProgressChanged(
        progressFraction = currentProgress,
        discoveredCount = _discoveredEvidence.size
      )
    )

    return discovered
  }

  /**
   * Evaluates all registered story facts against discovered evidence to unlock any new truths.
   */
  private fun evaluateStoryFacts(eventBus: GameEventBus?) {
    val discoveredIds = _discoveredEvidence.keys

    for ((factId, fact) in _storyFacts) {
      if (_unlockedFacts.containsKey(factId)) continue

      if (fact.canUnlock(discoveredIds)) {
        val unlocked = fact.copy(
          isUnlocked = true,
          unlockedTimestamp = System.currentTimeMillis()
        )
        _unlockedFacts[factId] = unlocked
        eventBus?.emit(
          GameEvent.FactUnlocked(
            factId = unlocked.id,
            title = unlocked.title
          )
        )
      }
    }
  }

  fun isEvidenceDiscovered(evidenceId: String): Boolean {
    return _discoveredEvidence.containsKey(evidenceId)
  }

  fun getEvidence(evidenceId: String): EvidenceItem? = _evidenceCatalog[evidenceId]

  fun unlockFact(factId: String, eventBus: GameEventBus? = null): Boolean {
    if (_unlockedFacts.containsKey(factId)) return false
    val fact = _storyFacts[factId] ?: return false
    val unlocked = fact.copy(
      isUnlocked = true,
      unlockedTimestamp = System.currentTimeMillis()
    )
    _unlockedFacts[factId] = unlocked
    eventBus?.emit(
      GameEvent.FactUnlocked(
        factId = unlocked.id,
        title = unlocked.title
      )
    )
    return true
  }

  fun isFactUnlocked(factId: String): Boolean {
    return _unlockedFacts.containsKey(factId)
  }

  fun getDiscoveredEvidence(): List<EvidenceItem> {
    return _discoveredEvidence.values.sortedByDescending { it.discoveredTimestamp }
  }

  fun getDiscoveredEvidenceByCategory(category: JournalCategory): List<EvidenceItem> {
    return _discoveredEvidence.values
      .filter { it.category == category }
      .sortedByDescending { it.discoveredTimestamp }
  }

  fun getDiscoveredFacts(): List<StoryFact> {
    return _unlockedFacts.values.sortedByDescending { it.unlockedTimestamp }
  }

  fun getUnlockedFactIds(): List<String> = _unlockedFacts.keys.toList()

  fun getAllStoryFactsWithStatus(): List<StoryFact> {
    val discoveredIds = _discoveredEvidence.keys
    return _storyFacts.values.map { fact ->
      val unlocked = _unlockedFacts[fact.id]
      if (unlocked != null) {
        unlocked
      } else {
        fact.copy(isUnlocked = fact.canUnlock(discoveredIds))
      }
    }
  }

  fun getDiscoveredCount(): Int = _discoveredEvidence.size

  fun getUnlockedFactCount(): Int = _unlockedFacts.size

  /**
   * Calculates overall investigation completion fraction based on catalogued items and facts.
   */
  fun getProgressFraction(): Float {
    val totalElements = _evidenceCatalog.size + _storyFacts.size
    if (totalElements == 0) return 0.0f
    val discoveredElements = _discoveredEvidence.size + _unlockedFacts.size
    return (discoveredElements.toFloat() / totalElements).coerceIn(0.0f, 1.0f)
  }

  fun getSnapshot(): InvestigationState {
    return InvestigationState(
      discoveredEvidenceIds = _discoveredEvidence.keys.toSet(),
      discoveredFactIds = _unlockedFacts.keys.toSet(),
      examinedObjectIds = _examinedObjects.toSet(),
      totalCatalogEvidenceCount = _evidenceCatalog.size,
      totalCatalogFactCount = _storyFacts.size,
      progressFraction = getProgressFraction()
    )
  }

  // SaveableEntity persistence
  override fun captureState(): Map<String, String> {
    return mapOf(
      "entityId" to entityId,
      "discoveredEvidence" to _discoveredEvidence.keys.joinToString(","),
      "unlockedFacts" to _unlockedFacts.keys.joinToString(","),
      "examinedObjects" to _examinedObjects.joinToString(",")
    )
  }

  override fun restoreState(state: Map<String, String>) {
    _discoveredEvidence.clear()
    _unlockedFacts.clear()
    _examinedObjects.clear()

    state["examinedObjects"]?.split(",")?.filter { it.isNotBlank() }?.let {
      _examinedObjects.addAll(it)
    }

    state["discoveredEvidence"]?.split(",")?.filter { it.isNotBlank() }?.forEach { id ->
      _evidenceCatalog[id]?.let {
        _discoveredEvidence[id] = it.copy(isDiscovered = true)
      }
    }

    state["unlockedFacts"]?.split(",")?.filter { it.isNotBlank() }?.forEach { id ->
      _storyFacts[id]?.let {
        _unlockedFacts[id] = it.copy(isUnlocked = true)
      }
    }
  }
}
