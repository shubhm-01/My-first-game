package com.example.story

import com.example.investigation.AcademyEvidenceCatalog

/**
 * Repository of Aether memory fragments, supporting deterministic variation across playthroughs
 * while guaranteeing that core story truths (such as "The Academy was deliberately sealed")
 * are uncompromised and fully corroborative.
 */
object VariableMemoryPool {

  // Memory Identifiers
  const val MEMORY_COUNCIL_QUARANTINE = "mem_council_quarantine_directive"
  const val MEMORY_FLEEING_RESEARCHER = "mem_fleeing_researcher_plea"
  const val MEMORY_SCRIBE_LOCKDOWN = "mem_scribe_lockdown_record"

  /**
   * Primary Variant: The High Council Emergency Lockdown Directive.
   * Player sees and hears senior archivist Jeremy and the senior scholar receiving
   * the emergency seal order from the High Council.
   */
  val VARIANT_COUNCIL_QUARANTINE = MemoryFragment(
    id = MEMORY_COUNCIL_QUARANTINE,
    title = "Echo of the Council's Mandate",
    associatedLocationId = StoryLocation.LOCATION_RESTRICTED_ARCHIVE,
    associatedFactIds = listOf("FACT_ACADEMY_DELIBERATELY_SEALED", AcademyEvidenceCatalog.FACT_RESTRICTED_WING_SEALED),
    triggerCondition = MemoryTriggerCondition.OnObjectExamined("prop_resonating_aether_relic"),
    sensoryEcho = "A blinding pulse of cerulean Aether crystallizes the air. Phantom figures in academic robes materialize around the archive table, panicked and speaking in hushed, urgent tones.",
    revelationStatement = "The Academy was deliberately sealed from the outside by Council decree — not by accident, but to entomb Elias and his research.",
    dialogueLines = listOf(
      MemoryDialogue("Archivist Jeremy", "The harmonic matrix is destabilizing! Master Elias refuses to halt the transference sequence!", 0.5f),
      MemoryDialogue("Senior Scholar", "The High Council has already made their decision. Drop the iron portcullis. Seal the Restricted Wing from the exterior!", 2.8f),
      MemoryDialogue("Archivist Jeremy", "From the exterior?! By the Heavens... they aren't containing the research. They are burying us alive with it!", 5.5f)
    ),
    durationSeconds = 8.5f,
    variantTag = "council_mandate"
  )

  /**
   * Alternate Variant: The Fleeing Researcher's Desperation.
   * Triggerable via examining the overturned research desk in the West Bay.
   */
  val VARIANT_FLEEING_RESEARCHER = MemoryFragment(
    id = MEMORY_FLEEING_RESEARCHER,
    title = "Echo of the Abandoned Trial",
    associatedLocationId = StoryLocation.LOCATION_RESEARCH_BAY,
    associatedFactIds = listOf("FACT_ACADEMY_DELIBERATELY_SEALED"),
    triggerCondition = MemoryTriggerCondition.OnObjectExamined("prop_sealed_research_desk"),
    sensoryEcho = "A sharp metallic clatter echoes through the workshop. A ghostly silhouette frantically shoves notebooks into an iron trunk before looking toward the sealed northern archway in horror.",
    revelationStatement = "Researchers were locked inside when the Council activated the outer wards without warning.",
    dialogueLines = listOf(
      MemoryDialogue("Junior Researcher", "The ward mechanisms are engaging from outside! Who authorized the perimeter lock?!", 0.5f),
      MemoryDialogue("Apprentice Raymond", "The Council... they saw the crystalline growth. They've decided none of us may leave.", 3.0f),
      MemoryDialogue("Junior Researcher", "We must hide the records in the archive before the seals solidify...", 5.8f)
    ),
    durationSeconds = 8.5f,
    variantTag = "fleeing_researcher"
  )

  /**
   * Alternate Variant: Security Scribe's Inscribed Warning.
   */
  val VARIANT_SCRIBE_LOCKDOWN = MemoryFragment(
    id = MEMORY_SCRIBE_LOCKDOWN,
    title = "Echo of the Final Gate Seal",
    associatedLocationId = StoryLocation.LOCATION_DEEP_GATE,
    associatedFactIds = listOf("FACT_ACADEMY_DELIBERATELY_SEALED"),
    triggerCondition = MemoryTriggerCondition.OnObjectExamined("prop_warning_inscription"),
    sensoryEcho = "The cold bronze plaque resonates with a deep vibration. Faint screams and the grinding of massive gears echo through the corridor.",
    revelationStatement = "Heavy iron gates were permanently barred by order of the Grand Inquisitor to quarantine Elias's breakthrough.",
    dialogueLines = listOf(
      MemoryDialogue("Gatekeeper", "Lock the portcullis! Quench the ignition lines!", 0.5f),
      MemoryDialogue("Scribe", "There are scholars still in the experimental bay!", 2.8f),
      MemoryDialogue("Gatekeeper", "Their lives are already forfeit to the Aether. Turn the key.", 5.2f)
    ),
    durationSeconds = 8.0f,
    variantTag = "scribe_lockdown"
  )

  val ALL_CERTIFIED_VARIANTS = listOf(
    VARIANT_COUNCIL_QUARANTINE,
    VARIANT_FLEEING_RESEARCHER,
    VARIANT_SCRIBE_LOCKDOWN
  )

  /**
   * Selects a primary memory fragment deterministically using a seed while ensuring
   * that the foundational story truth remains identical.
   */
  fun selectMemoryForSeed(seed: Long): MemoryFragment {
    val index = (seed.ushr(16) % ALL_CERTIFIED_VARIANTS.size).toInt().coerceAtLeast(0)
    return ALL_CERTIFIED_VARIANTS[index]
  }
}
