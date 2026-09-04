package com.example

import com.example.collision.CollisionSystem
import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.investigation.AcademyEvidenceCatalog
import com.example.investigation.InvestigationManager
import com.example.player.Position3D
import com.example.story.AetherPresenceSystem
import com.example.story.MemoryManager
import com.example.story.StoryChapter
import com.example.story.StoryLocation
import com.example.story.StoryProgression
import com.example.story.VariableMemoryPool
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
class Phase5StoryProgressionTest {

  @Test
  fun testSpatialLocationDetectionAndDiscovery() {
    val progression = StoryProgression()
    val eventBus = GameEventBus()

    // 1. Initially inside Atrium
    val atriumLoc = StoryLocation.fromPosition(0.0f, 0.0f)
    assertEquals(StoryLocation.ACADEMY_ATRIUM, atriumLoc)

    // Moving within Atrium
    progression.onPlayerMove(0.0f, 0.0f, eventBus)
    assertEquals(StoryLocation.ACADEMY_ATRIUM, progression.currentLocation)
    assertTrue(progression.discoveredLocations.contains(StoryLocation.ACADEMY_ATRIUM.id))

    // 2. Traversal into Restricted Corridor (Z = -7.0)
    val corridorLoc = StoryLocation.fromPosition(0.0f, -7.0f)
    assertEquals(StoryLocation.RESTRICTED_WING_CORRIDOR, corridorLoc)

    progression.onPlayerMove(0.0f, -7.0f, eventBus)
    assertEquals(StoryLocation.RESTRICTED_WING_CORRIDOR, progression.currentLocation)
    assertTrue(progression.discoveredLocations.contains(StoryLocation.RESTRICTED_WING_CORRIDOR.id))

    // 3. Traversal into Abandoned Research Bay (X = -3.5, Z = -8.5)
    val labLoc = StoryLocation.fromPosition(-3.5f, -8.5f)
    assertEquals(StoryLocation.ABANDONED_RESEARCH_BAY, labLoc)

    progression.onPlayerMove(-3.5f, -8.5f, eventBus)
    assertEquals(StoryLocation.ABANDONED_RESEARCH_BAY, progression.currentLocation)
    assertTrue(progression.discoveredLocations.contains(StoryLocation.ABANDONED_RESEARCH_BAY.id))

    // 4. Traversal into Containment Archive (X = 3.5, Z = -8.5)
    val archiveLoc = StoryLocation.fromPosition(3.5f, -8.5f)
    assertEquals(StoryLocation.CONTAINMENT_RECORDS_ARCHIVE, archiveLoc)

    progression.onPlayerMove(3.5f, -8.5f, eventBus)
    assertEquals(StoryLocation.CONTAINMENT_RECORDS_ARCHIVE, progression.currentLocation)
    assertTrue(progression.discoveredLocations.contains(StoryLocation.CONTAINMENT_RECORDS_ARCHIVE.id))

    // 5. Approaching Subterranean Sanctum Gate (X = 0.0, Z = -13.0)
    val gateLoc = StoryLocation.fromPosition(0.0f, -13.0f)
    assertEquals(StoryLocation.DEEP_SUBTERRANEAN_GATE, gateLoc)

    progression.onPlayerMove(0.0f, -13.0f, eventBus)
    assertEquals(StoryLocation.DEEP_SUBTERRANEAN_GATE, progression.currentLocation)
    assertTrue(progression.discoveredLocations.contains(StoryLocation.DEEP_SUBTERRANEAN_GATE.id))
  }

  @Test
  fun testStoryProgressionChapterAdvancement() {
    val progression = StoryProgression()
    val eventBus = GameEventBus()

    // Initially at CHAPTER 0: THE SEALED ACADEMY
    assertEquals(StoryChapter.CHAPTER_0_AWAKENING, progression.currentChapter)

    // Not ready to advance yet
    val advancedEarly = progression.advanceChapterIfReady(
      evidenceCount = 0,
      unlockedFacts = emptyList(),
      eventBus = eventBus
    )
    assertFalse(advancedEarly)
    assertEquals(StoryChapter.CHAPTER_0_AWAKENING, progression.currentChapter)

    // Player solves the first puzzle and unlocks the seal fact
    progression.setFlag("flag_first_puzzle_solved", true)
    val unlockedFacts = listOf(AcademyEvidenceCatalog.FACT_RESTRICTED_WING_SEALED)

    val advancedToCh1 = progression.advanceChapterIfReady(
      evidenceCount = 3,
      unlockedFacts = unlockedFacts,
      eventBus = eventBus
    )
    assertTrue("Should advance to Chapter 1 after solving first puzzle", advancedToCh1)
    assertEquals(StoryChapter.CHAPTER_1_REVELATION, progression.currentChapter)
    assertEquals(1, progression.chapterIndex)
  }

  @Test
  fun testDeterministicVariableMemoryPool() {
    // Deterministic selection: seed A consistently produces the exact same variant
    val variantRun1 = VariableMemoryPool.selectMemoryForSeed(seed = 1001L)
    val variantRun2 = VariableMemoryPool.selectMemoryForSeed(seed = 1001L)
    assertEquals(variantRun1.id, variantRun2.id)
    assertEquals(variantRun1.variantTag, variantRun2.variantTag)
    assertEquals(variantRun1.revelationStatement, variantRun2.revelationStatement)

    // All certified variants convey the essential story truth
    VariableMemoryPool.ALL_CERTIFIED_VARIANTS.forEach { variant ->
      assertTrue(variant.dialogueLines.isNotEmpty())
      assertTrue(variant.associatedFactIds.contains("FACT_ACADEMY_DELIBERATELY_SEALED"))
    }
  }

  @Test
  fun testMemoryPlaybackLifecycle() {
    val eventBus = GameEventBus()
    val progression = StoryProgression()
    val investigationManager = AcademyEvidenceCatalog.createDefaultInvestigationManager()
    val memoryManager = MemoryManager(storyProgression = progression, investigationManager = investigationManager)
    memoryManager.registerDefaultMemories(seed = 42819L)

    // Trigger council memory
    val triggered = memoryManager.triggerMemory("mem_council_mandate")
    assertTrue(triggered)
    assertNotNull(memoryManager.activeMemory)
    assertEquals(0.0f, memoryManager.playbackElapsed, 0.001f)

    // Fast-forward 2.5 seconds
    val active1 = memoryManager.update(2.5f)
    assertNotNull(active1)
    assertEquals(2.5f, memoryManager.playbackElapsed, 0.001f)

    // Fast-forward beyond total duration (10.0s)
    val activeEnd = memoryManager.update(10.0f)
    assertNull("Memory should conclude when elapsed exceeds total duration", activeEnd)
    assertFalse(memoryManager.isMemoryPlaying())

    // Check that memory is marked discovered
    assertTrue(memoryManager.isMemoryDiscovered("mem_council_mandate"))
    assertEquals(1, memoryManager.getDiscoveredMemories().size)
  }

  @Test
  fun testRestrictedWingCollisionAndBoundaries() {
    val collision = CollisionSystem()

    // 1. With door closed, player cannot cross into restricted corridor (Z < -5.65)
    assertFalse(collision.isDoorOpen)
    assertTrue("Closed door should block passage to Z = -6.5", collision.isColliding(0.0f, -6.5f))

    // 2. Open door
    collision.setDoorBarrier(isOpen = true)
    assertTrue(collision.isDoorOpen)

    // Now player CAN enter doorway and corridor
    assertFalse("Open doorway allows passage into corridor at Z = -7.0", collision.isColliding(0.0f, -7.0f))
    assertFalse("Corridor at Z = -10.0 is walkable", collision.isColliding(0.0f, -10.0f))

    // 3. West Research Bay is accessible through doorway at Z = -8.5
    assertFalse("West Research Bay entrance is walkable", collision.isColliding(-2.5f, -8.5f))
    // Overturned desk at (-3.5, -8.5) collides
    assertTrue("Desk in West Bay collides", collision.isColliding(-3.5f, -8.5f))

    // 4. East Archive is accessible through doorway at Z = -8.5
    assertFalse("East Archive center is walkable", collision.isColliding(2.5f, -8.5f))
    // Resonating relic plinth collides
    assertTrue("Relic plinth collides", collision.isColliding(3.5f, -9.8f))

    // 5. Far north subterranean gate at Z = -14.0 blocks passage beyond
    assertTrue("Barred deep gate blocks passage beyond Z = -14.0", collision.isColliding(0.0f, -14.5f))

    // 6. Exterior boundaries of Restricted Wing are strictly enforced
    assertTrue("Beyond West Bay outer wall should collide", collision.isColliding(-7.0f, -8.5f))
    assertTrue("Beyond East Archive outer wall should collide", collision.isColliding(7.0f, -8.5f))
  }

  @Test
  fun testAetherPresenceSystemProximity() {
    val eventBus = GameEventBus()
    val aetherSystem = AetherPresenceSystem(audioController = null, eventBus = eventBus)

    // Player far away in Atrium
    val atriumPos = Position3D(0.0f, 1.7f, 0.0f)
    val cueAtrium = aetherSystem.update(atriumPos, currentTimeMs = 1000L)
    assertNull("No Aether phenomenon should trigger in safe Atrium center", cueAtrium)

    // Player approaches Deep Subterranean Gate (Z = -13.0)
    val gatePos = Position3D(0.0f, 1.7f, -13.0f)
    val cueGate = aetherSystem.update(gatePos, currentTimeMs = 2000L)
    assertNotNull("Aether phenomenon should trigger near deep gate", cueGate)
    assertTrue(cueGate?.contains("vibration") == true || cueGate?.contains("ozone") == true)
  }

  @Test
  fun testRestrictedWingEvidenceAndFactCorroboration() {
    val manager = AcademyEvidenceCatalog.createDefaultInvestigationManager()
    val eventBus = GameEventBus()

    // Discover the two critical Restricted Wing quarantine documents
    manager.discoverEvidence(AcademyEvidenceCatalog.EVIDENCE_SEALED_RESEARCH_LOG, eventBus)
    manager.discoverEvidence(AcademyEvidenceCatalog.EVIDENCE_EMERGENCY_SECURITY_LOG, eventBus)

    // Fact: FACT_ACADEMY_DELIBERATELY_SEALED requires 2 critical clues
    assertTrue(
      "Council Directive + Security Log should reconstruct the Deliberate Sealing fact",
      manager.isFactUnlocked(AcademyEvidenceCatalog.FACT_ACADEMY_DELIBERATELY_SEALED)
    )

    val fact = manager.storyFacts[AcademyEvidenceCatalog.FACT_ACADEMY_DELIBERATELY_SEALED]
    assertNotNull(fact)
    assertTrue(fact?.statement?.contains("deliberately sealed") == true)
  }
}
