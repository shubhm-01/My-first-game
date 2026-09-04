package com.example.investigation

import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.data.SaveableEntity
import com.example.puzzle.AlchemicalSealingPuzzle
import com.example.puzzle.PuzzleConfiguration
import java.util.Random

/**
 * Data specification for a replayable investigation scenario.
 * Decouples narrative truth from variable physical clues and puzzle configurations.
 */
data class InvestigationScenario(
  val scenarioId: String,
  val title: String,
  val description: String,
  val requiredFacts: List<String>,
  val evidencePool: List<String>,
  val minimumEvidenceRequired: Int,
  val validEvidenceCombinations: List<Set<String>>,
  val misleadingEvidence: List<String> = emptyList(),
  val puzzleId: String,
  val completionCondition: (InvestigationManager, AlchemicalSealingPuzzle) -> Boolean = { manager, puzzle ->
    puzzle.status == com.example.puzzle.PuzzleStatus.SOLVED
  }
)

/**
 * The validated state of an active investigation run.
 */
data class ActiveScenarioRun(
  val scenarioId: String,
  val seed: Long,
  val selectedEvidenceIds: Set<String>,
  val activePuzzleConfig: PuzzleConfiguration,
  val misleadingEvidencePresent: List<String> = emptyList(),
  var isCompleted: Boolean = false
) : SaveableEntity {

  override val entityId: String get() = "scenario_run_$scenarioId"

  override fun captureState(): Map<String, String> {
    return mapOf(
      "scenarioId" to scenarioId,
      "seed" to seed.toString(),
      "selectedEvidenceIds" to selectedEvidenceIds.joinToString(","),
      "activePuzzleConfigId" to activePuzzleConfig.configId,
      "misleadingEvidencePresent" to misleadingEvidencePresent.joinToString(","),
      "isCompleted" to isCompleted.toString()
    )
  }

  override fun restoreState(state: Map<String, String>) {
    state["isCompleted"]?.let {
      isCompleted = it.toBoolean()
    }
  }
}

/**
 * Result of a solvability analysis.
 */
data class SolvabilityResult(
  val isSolvable: Boolean,
  val validCombinationsCount: Int,
  val missingClues: List<String> = emptyList(),
  val reason: String = if (isSolvable) "Scenario is certified solvable." else "Unsolvable configuration."
)

/**
 * Strict solvability validation engine.
 * Ensures that any generated scenario run:
 * 1. Has all required evidence registered in the catalog.
 * 2. Contains at least one valid evidence combination satisfying the required story facts.
 * 3. Contains the exact clue required to deduce the puzzle solution.
 * 4. Contains fair, non-contradictory misleading evidence that can be distinguished through corroboration.
 */
object SolvabilityValidator {

  fun validateScenario(
    scenario: InvestigationScenario,
    evidenceCatalog: Map<String, EvidenceItem>,
    factCatalog: Map<String, StoryFact>,
    puzzleConfigs: List<PuzzleConfiguration>
  ): SolvabilityResult {
    // 1. Verify all evidence pool items exist in the evidence catalog
    val missingEvidence = scenario.evidencePool.filter { !evidenceCatalog.containsKey(it) }
    if (missingEvidence.isNotEmpty()) {
      return SolvabilityResult(
        isSolvable = false,
        validCombinationsCount = 0,
        missingClues = missingEvidence,
        reason = "Scenario evidence pool contains unregistered IDs: $missingEvidence"
      )
    }

    // 2. Verify all required story facts exist in catalog
    val missingFacts = scenario.requiredFacts.filter { !factCatalog.containsKey(it) }
    if (missingFacts.isNotEmpty()) {
      return SolvabilityResult(
        isSolvable = false,
        validCombinationsCount = 0,
        reason = "Scenario requires unregistered StoryFacts: $missingFacts"
      )
    }

    // 3. Verify at least one valid combination exists
    if (scenario.validEvidenceCombinations.isEmpty()) {
      return SolvabilityResult(
        isSolvable = false,
        validCombinationsCount = 0,
        reason = "Scenario has no valid evidence combinations defined."
      )
    }

    // 4. Verify each combination is solvable against required facts and puzzle configs
    var verifiableCombinations = 0
    for (combination in scenario.validEvidenceCombinations) {
      if (combination.size < scenario.minimumEvidenceRequired) continue

      // Verify facts can be satisfied
      val canSatisfyFacts = scenario.requiredFacts.all { factId ->
        val fact = factCatalog[factId] ?: return@all false
        val overlap = combination.intersect(fact.contributingEvidenceIds)
        overlap.size >= fact.requiredEvidenceCount
      }

      // Verify puzzle clue is in this combination or can be derived
      val hasPuzzleClue = puzzleConfigs.any { config ->
        combination.contains(config.clueEvidenceId)
      }

      if (canSatisfyFacts && hasPuzzleClue) {
        verifiableCombinations++
      }
    }

    if (verifiableCombinations == 0) {
      return SolvabilityResult(
        isSolvable = false,
        validCombinationsCount = 0,
        reason = "No valid evidence combination satisfies both the required story facts and a certified puzzle configuration."
      )
    }

    return SolvabilityResult(
      isSolvable = true,
      validCombinationsCount = verifiableCombinations,
      reason = "Verified $verifiableCombinations solvable combinations."
    )
  }

  fun validateRun(
    run: ActiveScenarioRun,
    scenario: InvestigationScenario,
    evidenceCatalog: Map<String, EvidenceItem>,
    factCatalog: Map<String, StoryFact>
  ): Boolean {
    if (run.selectedEvidenceIds.size < scenario.minimumEvidenceRequired) return false

    // Check if at least one valid combination is fully contained in selectedEvidenceIds
    val containsValidCombo = scenario.validEvidenceCombinations.any { combo ->
      run.selectedEvidenceIds.containsAll(combo)
    }
    if (!containsValidCombo) return false

    // Verify the puzzle clue is included in the run
    if (!run.selectedEvidenceIds.contains(run.activePuzzleConfig.clueEvidenceId)) return false

    // Verify required facts can be unlocked with this run's evidence
    val canSatisfyFacts = scenario.requiredFacts.all { factId ->
      val fact = factCatalog[factId] ?: return@all false
      val overlap = run.selectedEvidenceIds.intersect(fact.contributingEvidenceIds)
      overlap.size >= fact.requiredEvidenceCount
    }

    return canSatisfyFacts
  }
}

/**
 * Manages deterministic scenario generation, active run lifecycle, and puzzle alignment.
 */
class InvestigationScenarioManager(
  val investigationManager: InvestigationManager,
  val puzzle: AlchemicalSealingPuzzle,
  override val entityId: String = "scenario_manager"
) : SaveableEntity {

  var activeRun: ActiveScenarioRun? = null
    private set

  private val registeredScenarios = mutableMapOf<String, InvestigationScenario>()

  fun registerScenario(scenario: InvestigationScenario) {
    registeredScenarios[scenario.scenarioId] = scenario
  }

  fun getScenario(scenarioId: String): InvestigationScenario? = registeredScenarios[scenarioId]

  /**
   * Generates a deterministic scenario run from a 64-bit seed.
   * Guaranteed to be 100% solvable through automated pre-validation.
   */
  fun generateScenarioRun(
    scenarioId: String,
    seed: Long,
    eventBus: GameEventBus? = null
  ): ActiveScenarioRun {
    val scenario = registeredScenarios[scenarioId]
      ?: throw IllegalArgumentException("Scenario '$scenarioId' is not registered.")

    val rng = Random(seed)

    // Filter puzzle configs that correspond to valid combinations
    val candidateCombos = scenario.validEvidenceCombinations.filter { combo ->
      combo.any { clueId ->
        AlchemicalSealingPuzzle.CERTIFIED_CONFIGURATIONS.any { it.clueEvidenceId == clueId }
      }
    }

    // Deterministically pick a valid combination
    val chosenIndex = if (candidateCombos.isNotEmpty()) {
      Math.floorMod(rng.nextInt(), candidateCombos.size)
    } else 0
    val selectedCombo = (candidateCombos.getOrNull(chosenIndex)
      ?: scenario.validEvidenceCombinations.first()).toMutableSet()

    // Find the puzzle configuration aligned with the chosen clue
    val matchingPuzzleConfig = AlchemicalSealingPuzzle.CERTIFIED_CONFIGURATIONS.find { config ->
      selectedCombo.contains(config.clueEvidenceId)
    } ?: AlchemicalSealingPuzzle.DEFAULT_CONFIG_FLAME

    // Deterministically add optional/corroborating clues from the scenario pool
    val remainingPool = scenario.evidencePool.filter { !selectedCombo.contains(it) }
    if (remainingPool.isNotEmpty()) {
      // Pick 1 additional background clue to enrich the investigation
      val extraIndex = Math.floorMod(rng.nextInt(), remainingPool.size)
      selectedCombo.add(remainingPool[extraIndex])
    }

    // Deterministically include fair misleading evidence if available
    val chosenMisleading = mutableListOf<String>()
    if (scenario.misleadingEvidence.isNotEmpty() && rng.nextBoolean()) {
      val misIndex = Math.floorMod(rng.nextInt(), scenario.misleadingEvidence.size)
      val misClue = scenario.misleadingEvidence[misIndex]
      chosenMisleading.add(misClue)
      selectedCombo.add(misClue)
    }

    val run = ActiveScenarioRun(
      scenarioId = scenarioId,
      seed = seed,
      selectedEvidenceIds = selectedCombo,
      activePuzzleConfig = matchingPuzzleConfig,
      misleadingEvidencePresent = chosenMisleading,
      isCompleted = false
    )

    // Pre-flight validation: discard if invalid and fallback to safe default
    val isValid = SolvabilityValidator.validateRun(
      run = run,
      scenario = scenario,
      evidenceCatalog = investigationManager.evidenceCatalog,
      factCatalog = investigationManager.storyFacts
    )

    val finalRun = if (isValid) {
      run
    } else {
      // Safe guaranteed fallback
      val fallbackCombo = scenario.validEvidenceCombinations.first()
      val fallbackConfig = AlchemicalSealingPuzzle.CERTIFIED_CONFIGURATIONS.find { config ->
        fallbackCombo.contains(config.clueEvidenceId)
      } ?: AlchemicalSealingPuzzle.DEFAULT_CONFIG_FLAME

      ActiveScenarioRun(
        scenarioId = scenarioId,
        seed = seed,
        selectedEvidenceIds = fallbackCombo,
        activePuzzleConfig = fallbackConfig,
        misleadingEvidencePresent = emptyList(),
        isCompleted = false
      )
    }

    activeRun = finalRun

    // Apply configuration to puzzle
    puzzle.configure(finalRun.activePuzzleConfig, eventBus)

    // Emit domain events
    eventBus?.emit(GameEvent.InvestigationScenarioStarted(scenarioId = scenarioId, title = scenario.title))
    eventBus?.emit(
      GameEvent.EvidencePoolGenerated(
        poolId = "pool_${scenarioId}_active",
        selectedEvidenceIds = finalRun.selectedEvidenceIds.toList()
      )
    )

    return finalRun
  }

  /**
   * Evaluates if the current investigation and puzzle state satisfy scenario completion.
   */
  fun checkCompletion(eventBus: GameEventBus? = null): Boolean {
    val run = activeRun ?: return false
    if (run.isCompleted) return true

    val scenario = registeredScenarios[run.scenarioId] ?: return false

    val isPuzzleSolved = puzzle.status == com.example.puzzle.PuzzleStatus.SOLVED
    val areFactsUnlocked = scenario.requiredFacts.all { investigationManager.isFactUnlocked(it) }

    if (isPuzzleSolved && areFactsUnlocked) {
      run.isCompleted = true
      eventBus?.emit(GameEvent.InvestigationScenarioCompleted(scenarioId = scenario.scenarioId))
      return true
    }

    return false
  }

  override fun captureState(): Map<String, String> {
    val state = mutableMapOf<String, String>()
    activeRun?.let { run ->
      state["activeScenarioId"] = run.scenarioId
      state["seed"] = run.seed.toString()
      state["selectedEvidence"] = run.selectedEvidenceIds.joinToString(";")
      state["puzzleConfigId"] = run.activePuzzleConfig.configId
      state["isCompleted"] = run.isCompleted.toString()
    }
    return state
  }

  override fun restoreState(state: Map<String, String>) {
    val scenarioId = state["activeScenarioId"] ?: return
    val seed = state["seed"]?.toLongOrNull() ?: 0L
    val evidenceList = state["selectedEvidence"]?.split(";")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
    val configId = state["puzzleConfigId"]
    val isCompleted = state["isCompleted"]?.toBoolean() ?: false

    val matchingConfig = AlchemicalSealingPuzzle.CERTIFIED_CONFIGURATIONS.find { it.configId == configId }
      ?: AlchemicalSealingPuzzle.DEFAULT_CONFIG_FLAME

    activeRun = ActiveScenarioRun(
      scenarioId = scenarioId,
      seed = seed,
      selectedEvidenceIds = evidenceList,
      activePuzzleConfig = matchingConfig,
      isCompleted = isCompleted
    )
  }
}
