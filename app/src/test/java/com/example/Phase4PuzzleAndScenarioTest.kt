package com.example

import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.investigation.AcademyEvidenceCatalog
import com.example.investigation.EvidenceImportance
import com.example.investigation.InvestigationManager
import com.example.investigation.InvestigationScenario
import com.example.investigation.InvestigationScenarioManager
import com.example.investigation.SolvabilityValidator
import com.example.puzzle.AlchemicalSealingPuzzle
import com.example.puzzle.AlchemicalSymbol
import com.example.puzzle.PuzzleStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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
class Phase4PuzzleAndScenarioTest {

  @Test
  fun testAlchemicalPuzzleEvaluationAndStateLifecycle() = runTest {
    val eventBus = GameEventBus()
    val puzzle = AlchemicalSealingPuzzle(initialConfig = AlchemicalSealingPuzzle.DEFAULT_CONFIG_FLAME)

    assertEquals(PuzzleStatus.ACTIVE, puzzle.status)
    assertEquals(3, puzzle.currentSlots.size)
    assertEquals(null, puzzle.currentSlots[0])

    val emittedEvents = mutableListOf<GameEvent>()
    val job = launch {
      eventBus.events.collect { emittedEvents.add(it) }
    }
    kotlinx.coroutines.yield()

    // 1. Cycle slots
    val firstSym = puzzle.cycleSlot(0, forward = true)
    assertEquals(AlchemicalSymbol.SULFUR, firstSym)
    assertEquals(AlchemicalSymbol.SULFUR, puzzle.currentSlots[0])

    // Cycle backward
    val prevSym = puzzle.cycleSlot(0, forward = false)
    assertEquals(AlchemicalSymbol.SILVER, prevSym)

    // 2. Incorrect sequence evaluation
    puzzle.setSlot(0, AlchemicalSymbol.SALT)
    puzzle.setSlot(1, AlchemicalSymbol.SULFUR)
    puzzle.setSlot(2, AlchemicalSymbol.MERCURY)

    val wrongResult = puzzle.evaluateWithBus(eventBus)
    kotlinx.coroutines.yield()

    assertFalse("Incorrect sequence should fail evaluation", wrongResult)
    assertEquals(PuzzleStatus.ACTIVE, puzzle.status)
    assertEquals(false, puzzle.lastEvaluationSuccess)
    assertTrue("Should not emit PuzzleSolved for wrong sequence", emittedEvents.none { it is GameEvent.PuzzleSolved })

    // 3. Reset mechanism
    puzzle.resetWithBus(eventBus)
    kotlinx.coroutines.yield()

    assertTrue("All slots should be null after reset", puzzle.currentSlots.all { it == null })
    assertTrue("Should emit PuzzleReset", emittedEvents.any { it is GameEvent.PuzzleReset })

    // 4. Correct sequence for DEFAULT_CONFIG_FLAME: SULFUR -> MERCURY -> SALT
    puzzle.setSlot(0, AlchemicalSymbol.SULFUR)
    puzzle.setSlot(1, AlchemicalSymbol.MERCURY)
    puzzle.setSlot(2, AlchemicalSymbol.SALT)

    val correctResult = puzzle.evaluateWithBus(eventBus)
    kotlinx.coroutines.yield()

    assertTrue("Correct sequence should solve puzzle", correctResult)
    assertEquals(PuzzleStatus.SOLVED, puzzle.status)
    assertEquals(true, puzzle.lastEvaluationSuccess)

    // Verify domain events
    val solvedEvent = emittedEvents.filterIsInstance<GameEvent.PuzzleSolved>().firstOrNull()
    assertNotNull("Should emit PuzzleSolved event", solvedEvent)
    assertEquals(puzzle.puzzleId, solvedEvent?.puzzleId)

    val stateEvent = emittedEvents.filterIsInstance<GameEvent.PuzzleStateChanged>().firstOrNull()
    assertNotNull("Should emit PuzzleStateChanged event", stateEvent)
    assertTrue(stateEvent?.isSolved == true)

    job.cancel()
  }

  @Test
  fun testPuzzleStatePersistence() {
    val puzzle = AlchemicalSealingPuzzle(initialConfig = AlchemicalSealingPuzzle.DEFAULT_CONFIG_LUNAR)
    puzzle.setSlot(0, AlchemicalSymbol.SILVER)
    puzzle.setSlot(1, AlchemicalSymbol.SALT)
    puzzle.setSlot(2, AlchemicalSymbol.SULFUR)
    puzzle.evaluate()

    assertEquals(PuzzleStatus.SOLVED, puzzle.status)

    // Capture state
    val savedState = puzzle.captureState()
    assertEquals(PuzzleStatus.SOLVED.name, savedState["status"])
    assertEquals("symbol_silver", savedState["slot0"])
    assertEquals("symbol_salt", savedState["slot1"])
    assertEquals("symbol_sulfur", savedState["slot2"])

    // Restore into fresh instance
    val restoredPuzzle = AlchemicalSealingPuzzle(initialConfig = AlchemicalSealingPuzzle.DEFAULT_CONFIG_LUNAR)
    restoredPuzzle.restoreState(savedState)

    assertEquals(PuzzleStatus.SOLVED, restoredPuzzle.status)
    assertEquals(AlchemicalSymbol.SILVER, restoredPuzzle.currentSlots[0])
    assertEquals(AlchemicalSymbol.SALT, restoredPuzzle.currentSlots[1])
    assertEquals(AlchemicalSymbol.SULFUR, restoredPuzzle.currentSlots[2])
  }

  @Test
  fun testSolvabilityValidatorOnAcademyScenario() {
    val investigationManager = AcademyEvidenceCatalog.createDefaultInvestigationManager()
    val scenario = AcademyEvidenceCatalog.createRestrictedWingScenario()

    val result = SolvabilityValidator.validateScenario(
      scenario = scenario,
      evidenceCatalog = investigationManager.evidenceCatalog,
      factCatalog = investigationManager.storyFacts,
      puzzleConfigs = AlchemicalSealingPuzzle.CERTIFIED_CONFIGURATIONS
    )

    assertTrue("Restricted Wing scenario must be certified solvable: ${result.reason}", result.isSolvable)
    assertTrue("Must have multiple verifiable combinations", result.validCombinationsCount >= 2)
  }

  @Test
  fun testSolvabilityValidatorDetectsUnsolvableConfigurations() {
    val investigationManager = AcademyEvidenceCatalog.createDefaultInvestigationManager()

    // 1. Scenario with missing evidence ID
    val brokenScenarioMissingEvidence = InvestigationScenario(
      scenarioId = "broken_scenario_1",
      title = "Broken Scenario",
      description = "Invalid evidence ID test",
      requiredFacts = listOf(AcademyEvidenceCatalog.FACT_RESTRICTED_WING_SEALED),
      evidencePool = listOf("non_existent_clue_id"),
      minimumEvidenceRequired = 1,
      validEvidenceCombinations = listOf(setOf("non_existent_clue_id")),
      puzzleId = "puzzle_restricted_wing_seal"
    )

    val result1 = SolvabilityValidator.validateScenario(
      scenario = brokenScenarioMissingEvidence,
      evidenceCatalog = investigationManager.evidenceCatalog,
      factCatalog = investigationManager.storyFacts,
      puzzleConfigs = AlchemicalSealingPuzzle.CERTIFIED_CONFIGURATIONS
    )
    assertFalse("Validator must reject unregistered clues", result1.isSolvable)

    // 2. Scenario without valid combinations
    val brokenScenarioNoCombos = InvestigationScenario(
      scenarioId = "broken_scenario_2",
      title = "Broken Scenario 2",
      description = "Empty combinations",
      requiredFacts = listOf(AcademyEvidenceCatalog.FACT_RESTRICTED_WING_SEALED),
      evidencePool = listOf(AcademyEvidenceCatalog.EVIDENCE_WARD_PROTOCOL_NOTE),
      minimumEvidenceRequired = 1,
      validEvidenceCombinations = emptyList(),
      puzzleId = "puzzle_restricted_wing_seal"
    )

    val result2 = SolvabilityValidator.validateScenario(
      scenario = brokenScenarioNoCombos,
      evidenceCatalog = investigationManager.evidenceCatalog,
      factCatalog = investigationManager.storyFacts,
      puzzleConfigs = AlchemicalSealingPuzzle.CERTIFIED_CONFIGURATIONS
    )
    assertFalse("Validator must reject scenarios with no valid combinations", result2.isSolvable)
  }

  @Test
  fun testDeterministicRunGeneration() {
    val manager = AcademyEvidenceCatalog.createDefaultInvestigationManager()
    val puzzle = AlchemicalSealingPuzzle()
    val scenarioManager = InvestigationScenarioManager(manager, puzzle)

    val scenario = AcademyEvidenceCatalog.createRestrictedWingScenario()
    scenarioManager.registerScenario(scenario)

    // Run 1 with Seed 1042
    val runA1 = scenarioManager.generateScenarioRun(scenario.scenarioId, 1042L)

    // Run 2 with Seed 1042 (identical seed)
    val runA2 = scenarioManager.generateScenarioRun(scenario.scenarioId, 1042L)

    assertEquals("Same seed must yield identical puzzle configuration", runA1.activePuzzleConfig.configId, runA2.activePuzzleConfig.configId)
    assertEquals("Same seed must yield identical evidence set", runA1.selectedEvidenceIds, runA2.selectedEvidenceIds)

    // Verify run solvability against validator
    val isValid = SolvabilityValidator.validateRun(
      run = runA1,
      scenario = scenario,
      evidenceCatalog = manager.evidenceCatalog,
      factCatalog = manager.storyFacts
    )
    assertTrue("Generated run must be certified solvable", isValid)
  }

  @Test
  fun testEndToEndInvestigationAndPuzzleCompletionFlow() = runTest {
    val eventBus = GameEventBus()
    val investigationManager = AcademyEvidenceCatalog.createDefaultInvestigationManager()
    val puzzle = AlchemicalSealingPuzzle()
    val scenarioManager = InvestigationScenarioManager(investigationManager, puzzle)

    val scenario = AcademyEvidenceCatalog.createRestrictedWingScenario()
    scenarioManager.registerScenario(scenario)

    val emittedEvents = mutableListOf<GameEvent>()
    val job = launch {
      eventBus.events.collect { emittedEvents.add(it) }
    }
    kotlinx.coroutines.yield()

    // 1. Generate run with seed
    val run = scenarioManager.generateScenarioRun(scenario.scenarioId, 42819L, eventBus)
    kotlinx.coroutines.yield()

    assertNotNull(scenarioManager.activeRun)
    assertTrue("Should emit InvestigationScenarioStarted", emittedEvents.any { it is GameEvent.InvestigationScenarioStarted })
    assertTrue("Should emit EvidencePoolGenerated", emittedEvents.any { it is GameEvent.EvidencePoolGenerated })

    // 2. Discover the clues present in this run
    for (clueId in run.selectedEvidenceIds) {
      investigationManager.discoverEvidence(clueId, eventBus)
      kotlinx.coroutines.yield()
    }

    // 3. Verify story fact for the Restricted Wing is unlocked
    assertTrue(
      "Story fact FACT_RESTRICTED_WING_SEALED must be unlocked by discovered evidence",
      investigationManager.isFactUnlocked(AcademyEvidenceCatalog.FACT_RESTRICTED_WING_SEALED)
    )

    // 4. Before solving puzzle, scenario is not completed
    assertFalse(scenarioManager.checkCompletion(eventBus))

    // 5. Apply the target sequence specified in the active puzzle config
    val target = run.activePuzzleConfig.targetSequence
    for (i in target.indices) {
      puzzle.setSlot(i, target[i])
    }
    val solved = puzzle.evaluateWithBus(eventBus)
    kotlinx.coroutines.yield()

    assertTrue("Puzzle should be solved with target sequence", solved)
    assertEquals(PuzzleStatus.SOLVED, puzzle.status)

    // 6. Complete scenario
    val completed = scenarioManager.checkCompletion(eventBus)
    kotlinx.coroutines.yield()

    assertTrue("Scenario must be completed when facts unlocked and puzzle solved", completed)
    assertTrue("Should emit InvestigationScenarioCompleted", emittedEvents.any { it is GameEvent.InvestigationScenarioCompleted })

    job.cancel()
  }

  @Test
  fun testMisleadingEvidenceProperty() {
    val manager = AcademyEvidenceCatalog.createDefaultInvestigationManager()
    val draftItem = manager.getEvidence(AcademyEvidenceCatalog.EVIDENCE_OBSOLETE_DRAFT)

    assertNotNull("Apprentice draft item must exist in catalog", draftItem)
    assertEquals(EvidenceImportance.MISLEADING, draftItem?.importance)
  }
}
