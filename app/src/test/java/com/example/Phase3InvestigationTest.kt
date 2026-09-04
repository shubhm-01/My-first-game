package com.example

import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.interaction.AcademyDoor
import com.example.interaction.AlchemyTable
import com.example.interaction.CollectibleIngredient
import com.example.interaction.InteractionSystem
import com.example.investigation.AcademyEvidenceCatalog
import com.example.investigation.EvidenceImportance
import com.example.investigation.EvidenceItem
import com.example.investigation.EvidenceType
import com.example.investigation.InvestigationManager
import com.example.investigation.JournalCategory
import com.example.investigation.StoryFact
import com.example.investigation.VariableEvidencePool
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
class Phase3InvestigationTest {

  @Test
  fun testInvestigationManagerEvidenceRegistrationAndDiscovery() = runTest {
    val eventBus = GameEventBus()
    val manager = InvestigationManager()

    val testItem = EvidenceItem(
      id = "evidence_test_note",
      title = "Test Alchemical Note",
      description = "A hastily written test fragment.",
      type = EvidenceType.DOCUMENT,
      importance = EvidenceImportance.CRITICAL,
      category = JournalCategory.ALCHEMY
    )

    manager.registerEvidence(testItem)
    assertEquals(1, manager.evidenceCatalog.size)
    assertFalse(manager.isEvidenceDiscovered("evidence_test_note"))

    val emittedEvents = mutableListOf<GameEvent>()
    val job = launch {
      eventBus.events.collect { emittedEvents.add(it) }
    }
    kotlinx.coroutines.yield()

    // Discover first time
    val discovered = manager.discoverEvidence("evidence_test_note", eventBus)
    kotlinx.coroutines.yield()

    assertNotNull(discovered)
    assertEquals("evidence_test_note", discovered?.id)
    assertTrue(discovered!!.isDiscovered)
    assertTrue(discovered.discoveredTimestamp > 0L)
    assertTrue(manager.isEvidenceDiscovered("evidence_test_note"))
    assertEquals(1, manager.getDiscoveredCount())

    // Verify domain event emitted
    val ev = emittedEvents.filterIsInstance<GameEvent.EvidenceDiscovered>().firstOrNull()
    assertNotNull("Should emit EvidenceDiscovered event", ev)
    assertEquals("evidence_test_note", ev?.evidenceId)
    assertEquals("Test Alchemical Note", ev?.title)

    // Verify duplicate discovery prevention
    val duplicate = manager.discoverEvidence("evidence_test_note", eventBus)
    kotlinx.coroutines.yield()
    assertNull("Duplicate discovery should return null", duplicate)
    assertEquals(1, manager.getDiscoveredCount())

    job.cancel()
  }

  @Test
  fun testStoryFactUnlockingFromMultipleEvidence() = runTest {
    val eventBus = GameEventBus()
    val manager = AcademyEvidenceCatalog.createDefaultInvestigationManager()

    val factId = AcademyEvidenceCatalog.FACT_ELIAS_AETHER_EXPERIMENTS
    val fact = manager.storyFacts[factId]
    assertNotNull(fact)
    assertEquals(2, fact!!.requiredEvidenceCount)
    assertFalse(manager.isFactUnlocked(factId))

    val eventsList = mutableListOf<GameEvent>()
    val job = launch {
      eventBus.events.collect { eventsList.add(it) }
    }
    kotlinx.coroutines.yield()

    // Discover first piece of evidence
    val ev1 = manager.discoverEvidence(AcademyEvidenceCatalog.EVIDENCE_RESEARCH_NOTE, eventBus)
    kotlinx.coroutines.yield()
    assertNotNull(ev1)
    assertFalse("Fact should not unlock with only 1 piece", manager.isFactUnlocked(factId))

    // Discover second corroborating piece
    val ev2 = manager.discoverEvidence(AcademyEvidenceCatalog.EVIDENCE_AETHER_SYMBOL, eventBus)
    kotlinx.coroutines.yield()
    assertNotNull(ev2)
    assertTrue("Fact should now be unlocked with 2 pieces", manager.isFactUnlocked(factId))

    val hasFactEvent = eventsList.any {
      it is GameEvent.FactUnlocked && it.factId == factId
    }
    assertTrue("Should emit FactUnlocked event", hasFactEvent)

    job.cancel()
  }

  @Test
  fun testVariableEvidencePoolSolvabilityValidation() {
    val pool = VariableEvidencePool(
      poolId = "pool_test",
      requiredFactId = "FACT_TEST",
      possibleEvidenceIds = listOf("clue_1", "clue_2", "clue_3", "clue_4"),
      minimumEvidenceCount = 2,
      validCombinations = listOf(
        setOf("clue_1", "clue_2"),
        setOf("clue_3", "clue_4")
      ),
      optionalEvidenceIds = listOf("lore_clue_5")
    )

    // Test incomplete subset
    assertFalse("Subset with only clue_1 should not be solvable", pool.validateSolvability(setOf("clue_1")))
    assertFalse("Subset with mismatching clues should not be solvable", pool.validateSolvability(setOf("clue_1", "clue_3")))

    // Test valid subsets
    assertTrue("Combo 1 should be solvable", pool.validateSolvability(setOf("clue_1", "clue_2")))
    assertTrue("Combo 2 should be solvable", pool.validateSolvability(setOf("clue_3", "clue_4", "lore_clue_5")))

    // Test procedural sampling creates solvable set
    val sampled = pool.sampleSolvableConfiguration(rngSeed = 12345L)
    assertTrue("Sampled configuration must be solvable", pool.validateSolvability(sampled))
  }

  @Test
  fun testInvestigationManagerPersistenceCaptureAndRestore() {
    val original = AcademyEvidenceCatalog.createDefaultInvestigationManager()

    original.recordObjectExamined("prop_alchemy_table")
    original.discoverEvidence(AcademyEvidenceCatalog.EVIDENCE_RESEARCH_NOTE)
    original.discoverEvidence(AcademyEvidenceCatalog.EVIDENCE_AETHER_SYMBOL)
    assertTrue(original.isFactUnlocked(AcademyEvidenceCatalog.FACT_ELIAS_AETHER_EXPERIMENTS))

    // Capture state
    val savedState = original.captureState()
    assertNotNull(savedState["discoveredEvidence"])
    assertNotNull(savedState["unlockedFacts"])
    assertNotNull(savedState["examinedObjects"])

    // Restore in clean instance
    val restored = AcademyEvidenceCatalog.createDefaultInvestigationManager()
    restored.restoreState(savedState)

    assertTrue(restored.hasExaminedObject("prop_alchemy_table"))
    assertTrue(restored.isEvidenceDiscovered(AcademyEvidenceCatalog.EVIDENCE_RESEARCH_NOTE))
    assertTrue(restored.isEvidenceDiscovered(AcademyEvidenceCatalog.EVIDENCE_AETHER_SYMBOL))
    assertTrue(restored.isFactUnlocked(AcademyEvidenceCatalog.FACT_ELIAS_AETHER_EXPERIMENTS))
    assertEquals(2, restored.getDiscoveredCount())
    assertEquals(1, restored.getUnlockedFactCount())
  }

  @Test
  fun testInteractableEvidenceBindings() {
    val alchemyTable = AlchemyTable()
    assertEquals("evidence_research_note", alchemyTable.evidenceId)

    val collectible = CollectibleIngredient()
    assertEquals("evidence_silver_cutting", collectible.evidenceId)

    val interactionSystem = InteractionSystem()
    val cauldron = interactionSystem.findInteractable("prop_cauldron")
    assertNotNull(cauldron)
    assertEquals("evidence_ash_residue", cauldron?.evidenceId)

    val crystal = interactionSystem.findInteractable("prop_crystal")
    assertNotNull(crystal)
    assertEquals("evidence_aether_symbol", crystal?.evidenceId)

    val ledger = interactionSystem.findInteractable("prop_atrium_ledger")
    assertNotNull(ledger)
    assertEquals("evidence_atrium_ledger", ledger?.evidenceId)
  }
}
