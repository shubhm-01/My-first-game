package com.example.investigation

/**
 * Pre-configured evidence, facts, and variable clue pools for the Academy Atrium.
 * Strictly follows the locked narrative foundation from Stage A Story Bible.
 */
object AcademyEvidenceCatalog {

  // Story Fact Identifiers
  const val FACT_ELIAS_AETHER_EXPERIMENTS = "FACT_ELIAS_AETHER_EXPERIMENTS"
  const val FACT_ABRUPT_EVACUATION = "FACT_ABRUPT_EVACUATION"
  const val FACT_AETHER_REAGENTS = "FACT_AETHER_REAGENTS"
  const val FACT_RESTRICTED_WING_SEALED = "FACT_RESTRICTED_WING_SEALED"

  // Evidence Identifiers
  const val EVIDENCE_RESEARCH_NOTE = "evidence_research_note"
  const val EVIDENCE_AETHER_SYMBOL = "evidence_aether_symbol"
  const val EVIDENCE_ASH_RESIDUE = "evidence_ash_residue"
  const val EVIDENCE_SILVER_CUTTING = "evidence_silver_cutting"
  const val EVIDENCE_ATRIUM_LEDGER = "evidence_atrium_ledger"

  // Phase 4 Variable Investigation & Puzzle Evidence
  const val EVIDENCE_WARD_PROTOCOL_NOTE = "evidence_ward_protocol_note"
  const val EVIDENCE_SEALING_DIAGRAM = "evidence_sealing_diagram"
  const val EVIDENCE_PEDESTAL_INSCRIPTION = "evidence_pedestal_inscription"
  const val EVIDENCE_OBSOLETE_DRAFT = "evidence_obsolete_draft"

  // Phase 5 Story Progression & Restricted Wing Evidence
  const val FACT_ACADEMY_DELIBERATELY_SEALED = "FACT_ACADEMY_DELIBERATELY_SEALED"
  const val FACT_AETHER_MEMORIES_PERSIST = "FACT_AETHER_MEMORIES_PERSIST"

  const val EVIDENCE_SEALED_RESEARCH_LOG = "evidence_sealed_research_log"
  const val EVIDENCE_EMERGENCY_SECURITY_LOG = "evidence_emergency_security_log"
  const val EVIDENCE_BURNT_LAB_JOURNAL = "evidence_burnt_lab_journal"
  const val EVIDENCE_AETHER_RESONANCE_RELIC = "evidence_aether_resonance_relic"
  const val EVIDENCE_SCRIBE_WARNING_PLAQUE = "evidence_scribe_warning_plaque"

  // Scenario Identifiers
  const val SCENARIO_RESTRICTED_WING_SEALING = "scenario_restricted_wing_sealing"

  /**
   * Generates a fully populated [InvestigationManager] for Chapter 0 (Academy Atrium).
   */
  fun createDefaultInvestigationManager(): InvestigationManager {
    val manager = InvestigationManager()

    // 1. Story Facts
    manager.registerFact(
      StoryFact(
        id = FACT_ELIAS_AETHER_EXPERIMENTS,
        title = "Consciousness Preservation Research",
        statement = "Master Elias Veyr conducted secret alchemical research within the Academy, seeking to preserve human consciousness and memory beyond mortal physical death.",
        category = JournalCategory.ALCHEMY,
        contributingEvidenceIds = setOf(EVIDENCE_RESEARCH_NOTE, EVIDENCE_AETHER_SYMBOL),
        requiredEvidenceCount = 2,
        tags = listOf("elias", "aether", "origin")
      )
    )

    manager.registerFact(
      StoryFact(
        id = FACT_ABRUPT_EVACUATION,
        title = "Catastrophic Academy Sealing",
        statement = "The Academy was hastily evacuated and sealed from the outside following an uncontained experiment failure, leaving several researchers unaccounted for.",
        category = JournalCategory.INCIDENT,
        contributingEvidenceIds = setOf(EVIDENCE_ASH_RESIDUE, EVIDENCE_ATRIUM_LEDGER),
        requiredEvidenceCount = 2,
        tags = listOf("incident", "evacuation", "missing")
      )
    )

    manager.registerFact(
      StoryFact(
        id = FACT_AETHER_REAGENTS,
        title = "Consciousness Dampening Catalysts",
        statement = "Specific botanical distillates like silver herb were cultivated to suppress volatile resonance during consciousness transference trials.",
        category = JournalCategory.ALCHEMY,
        contributingEvidenceIds = setOf(EVIDENCE_SILVER_CUTTING),
        requiredEvidenceCount = 1,
        tags = listOf("alchemy", "reagents")
      )
    )

    manager.registerFact(
      StoryFact(
        id = FACT_RESTRICTED_WING_SEALED,
        title = "Deliberate Sealing of the Restricted Wing",
        statement = "The Restricted Wing entrance was locked with an alchemical ward requiring harmonic alignment across three fundamental principles (Soul, Mind, and Ground) to contain volatile Aether leakage.",
        category = JournalCategory.INCIDENT,
        contributingEvidenceIds = setOf(
          EVIDENCE_WARD_PROTOCOL_NOTE,
          EVIDENCE_SEALING_DIAGRAM,
          EVIDENCE_PEDESTAL_INSCRIPTION,
          EVIDENCE_ATRIUM_LEDGER
        ),
        requiredEvidenceCount = 2,
        tags = listOf("restricted_wing", "sealing", "puzzle")
      )
    )

    manager.registerFact(
      StoryFact(
        id = FACT_ACADEMY_DELIBERATELY_SEALED,
        title = "The Academy Was Deliberately Sealed",
        statement = "The catastrophic quarantine was not a mere accident: the Academy was deliberately sealed from the exterior by order of the High Council, trapping Master Elias and all subterranean researchers inside.",
        category = JournalCategory.INCIDENT,
        contributingEvidenceIds = setOf(
          EVIDENCE_SEALED_RESEARCH_LOG,
          EVIDENCE_EMERGENCY_SECURITY_LOG,
          EVIDENCE_BURNT_LAB_JOURNAL,
          EVIDENCE_AETHER_RESONANCE_RELIC,
          EVIDENCE_SCRIBE_WARNING_PLAQUE
        ),
        requiredEvidenceCount = 1,
        tags = listOf("council", "quarantine", "conspiracy", "incident")
      )
    )

    manager.registerFact(
      StoryFact(
        id = FACT_AETHER_MEMORIES_PERSIST,
        title = "Aether Memory Preservation",
        statement = "Concentrated Aether fields record human thoughts, words, and sensory impressions during extreme harmonic resonance, preserving playable memory echoes indefinitely.",
        category = JournalCategory.AETHER,
        contributingEvidenceIds = setOf(
          EVIDENCE_AETHER_RESONANCE_RELIC,
          EVIDENCE_AETHER_SYMBOL
        ),
        requiredEvidenceCount = 2,
        tags = listOf("aether", "memory", "resonance")
      )
    )

    // 2. Initial Physical & Environmental Evidence
    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_RESEARCH_NOTE,
        title = "Damaged Research Note",
        description = "A torn parchment written in Master Elias Veyr's deliberate script: '...containment resonance remains unstable during primary transference. The silver herb essence delays memory degradation, but cannot halt absorption into the Aether matrix...'",
        type = EvidenceType.DOCUMENT,
        importance = EvidenceImportance.CRITICAL,
        category = JournalCategory.ALCHEMY,
        relatedFactIds = listOf(FACT_ELIAS_AETHER_EXPERIMENTS),
        relatedCharacters = listOf("Elias Veyr"),
        relatedLocations = listOf("Alchemy Workbench")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_AETHER_SYMBOL,
        title = "Engraved Aether Sigil",
        description = "A spiraling geometric sigil deeply carved into the stone pedestal beneath the luminous crystal. Ancient alchemical notations symbolize an artificial receptacle engineered to anchor disembodied human consciousness.",
        type = EvidenceType.SYMBOL,
        importance = EvidenceImportance.SIGNIFICANT,
        category = JournalCategory.AETHER,
        relatedFactIds = listOf(FACT_ELIAS_AETHER_EXPERIMENTS),
        relatedCharacters = listOf("Elias Veyr"),
        relatedLocations = listOf("East Atrium Alcove")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_ASH_RESIDUE,
        title = "Cold Cauldron Ash Residue",
        description = "Dark mineral ash mixed with trace crystalline lavender deposits at the bottom of the bronze vessel. Indicates an urgent neutralization of volatile catalysts moments before the evacuation.",
        type = EvidenceType.EXPERIMENT,
        importance = EvidenceImportance.SIGNIFICANT,
        category = JournalCategory.INCIDENT,
        relatedFactIds = listOf(FACT_ABRUPT_EVACUATION),
        relatedCharacters = listOf("Academy Researchers"),
        relatedLocations = listOf("Bronze Cauldron")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_SILVER_CUTTING,
        title = "Preserved Silver Herb Cutting",
        description = "A rare lunar-cultivated flora with potent dampening properties. Elias's margin notes record its use to soothe consciousness agitation during preliminary transference trials.",
        type = EvidenceType.OBJECT,
        importance = EvidenceImportance.SIGNIFICANT,
        category = JournalCategory.ALCHEMY,
        relatedFactIds = listOf(FACT_AETHER_REAGENTS),
        relatedCharacters = listOf("Elias Veyr"),
        relatedLocations = listOf("Workbench Specimen Tray")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_ATRIUM_LEDGER,
        title = "Evacuation Ledger Fragment",
        description = "An academy registry sheet from the night the grand halls were sealed. Several research assistants' signatures are abruptly struck through in heavy black ink, with a frantic margin note: 'Do not follow them into the lower sanctum.'",
        type = EvidenceType.DOCUMENT,
        importance = EvidenceImportance.CRITICAL,
        category = JournalCategory.INCIDENT,
        relatedFactIds = listOf(FACT_ABRUPT_EVACUATION, FACT_RESTRICTED_WING_SEALED),
        relatedCharacters = listOf("Senior Researchers", "Elias Veyr"),
        relatedLocations = listOf("Archival Stand")
      )
    )

    // Phase 4 Evidence Items
    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_WARD_PROTOCOL_NOTE,
        title = "Flame Binding Directive",
        description = "An urgent emergency directive written by Elias: 'In event of uncontained Aether surge, align the door mechanism in exact harmonic order: First quell soul flame with [Sulfur], bind volatile currents in [Mercury], and anchor the barrier with [Salt].'",
        type = EvidenceType.DOCUMENT,
        importance = EvidenceImportance.CRITICAL,
        category = JournalCategory.ALCHEMY,
        relatedFactIds = listOf(FACT_RESTRICTED_WING_SEALED),
        relatedCharacters = listOf("Elias Veyr"),
        relatedLocations = listOf("Alchemy Workbench Drawer")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_SEALING_DIAGRAM,
        title = "Archival Sealing Diagram",
        description = "A technical schematic of the doorway mechanism: 'Emergency containment procedure: Quench volatile currents with [Silver], solidify the threshold with [Salt], and seal the gateway with [Sulfur].'",
        type = EvidenceType.DOCUMENT,
        importance = EvidenceImportance.CRITICAL,
        category = JournalCategory.INCIDENT,
        relatedFactIds = listOf(FACT_RESTRICTED_WING_SEALED),
        relatedCharacters = listOf("Academy Engineers"),
        relatedLocations = listOf("West Reading Lectern")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_PEDESTAL_INSCRIPTION,
        title = "Alchemical Sealing Inscription",
        description = "Weathered gilded lettering carved into the lock frame: 'Three principles hold the threshold sealed. When their harmonic balance is aligned to the master's protocol, the lock turns freely.'",
        type = EvidenceType.INSCRIPTION,
        importance = EvidenceImportance.SIGNIFICANT,
        category = JournalCategory.AETHER,
        relatedFactIds = listOf(FACT_RESTRICTED_WING_SEALED),
        relatedCharacters = listOf("Founding Alchemists"),
        relatedLocations = listOf("Restricted Wing Lock Housing")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_OBSOLETE_DRAFT,
        title = "Discarded Apprentice Draft",
        description = "An apprentice's crude proposal suggesting: 'Fill all three lock chambers with Salt stones for uniform density.' Stamped across in red alchemical ink: 'REJECTED — lacks tri-elemental polarity; would rupture the crystal matrix. Consult Master Elias's protocol.'",
        type = EvidenceType.DOCUMENT,
        importance = EvidenceImportance.MISLEADING,
        category = JournalCategory.INCIDENT,
        relatedFactIds = listOf(FACT_RESTRICTED_WING_SEALED),
        relatedCharacters = listOf("Apprentice Raymond"),
        relatedLocations = listOf("Discarded Specimen Shelf")
      )
    )

    // Phase 5 Restricted Wing & Story Progression Evidence
    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_SEALED_RESEARCH_LOG,
        title = "Council Quarantine Directive",
        description = "An emergency imperial order stamped with wax seals of the High Council: '...containment protocols initiated. Under no circumstances may the lower research levels be unsealed. Bar all transit. Entomb the anomaly with its creator.'",
        type = EvidenceType.DOCUMENT,
        importance = EvidenceImportance.CRITICAL,
        category = JournalCategory.INCIDENT,
        relatedFactIds = listOf(FACT_ACADEMY_DELIBERATELY_SEALED),
        relatedCharacters = listOf("High Council", "Elias Veyr"),
        relatedLocations = listOf("Abandoned Research Bay")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_EMERGENCY_SECURITY_LOG,
        title = "Containment Lock Failure Record",
        description = "An automated registry quill entry: 'Lockdown engaged from exterior relay 04. Interior override circuits severed. Pressure rising in subterranean labs. Evacuation halted by remote decree.'",
        type = EvidenceType.DOCUMENT,
        importance = EvidenceImportance.CRITICAL,
        category = JournalCategory.INCIDENT,
        relatedFactIds = listOf(FACT_ACADEMY_DELIBERATELY_SEALED),
        relatedCharacters = listOf("Academy Security"),
        relatedLocations = listOf("Containment Records Archive")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_BURNT_LAB_JOURNAL,
        title = "Burned Laboratory Journal",
        description = "Charred parchment salvaged from a shattered distillation mantle: '...they fear what Elias has discovered. They call it madness, but the crystalline resonance is alive. If you are reading this after the seals close, look to the resonance relics...'",
        type = EvidenceType.DOCUMENT,
        importance = EvidenceImportance.SIGNIFICANT,
        category = JournalCategory.ALCHEMY,
        relatedFactIds = listOf(FACT_ACADEMY_DELIBERATELY_SEALED, FACT_AETHER_MEMORIES_PERSIST),
        relatedCharacters = listOf("Elias Veyr"),
        relatedLocations = listOf("Abandoned Research Bay")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_AETHER_RESONANCE_RELIC,
        title = "Resonating Aether Core",
        description = "A hovering crystalline relic pulsing with an eerie cyan-violet luminosity. Touching the containment field triggers vivid, auditory echoes of the panicked researchers on the night the Academy was sealed.",
        type = EvidenceType.OBJECT,
        importance = EvidenceImportance.CRITICAL,
        category = JournalCategory.AETHER,
        relatedFactIds = listOf(FACT_ACADEMY_DELIBERATELY_SEALED, FACT_AETHER_MEMORIES_PERSIST),
        relatedCharacters = listOf("Archivist Jeremy", "Senior Scholar"),
        relatedLocations = listOf("Containment Records Archive")
      )
    )

    manager.registerEvidence(
      EvidenceItem(
        id = EVIDENCE_SCRIBE_WARNING_PLAQUE,
        title = "Containment Warning Plaque",
        description = "A heavy bronze plaque bolted beside the deep subterranean gate: 'WARNING: Subterranean sector under permanent imperial quarantine. No admittance under pain of instant dissolution.'",
        type = EvidenceType.INSCRIPTION,
        importance = EvidenceImportance.SIGNIFICANT,
        category = JournalCategory.INCIDENT,
        relatedFactIds = listOf(FACT_ACADEMY_DELIBERATELY_SEALED),
        relatedCharacters = listOf("Grand Inquisitor"),
        relatedLocations = listOf("Subterranean Sanctum Gate")
      )
    )

    // 3. Variable Evidence Pool Foundation
    manager.registerVariablePool(
      VariableEvidencePool(
        poolId = "pool_atrium_elias_origin",
        requiredFactId = FACT_ELIAS_AETHER_EXPERIMENTS,
        possibleEvidenceIds = listOf(
          EVIDENCE_RESEARCH_NOTE,
          EVIDENCE_AETHER_SYMBOL,
          "evidence_alt_elias_diary"
        ),
        minimumEvidenceCount = 2,
        validCombinations = listOf(
          setOf(EVIDENCE_RESEARCH_NOTE, EVIDENCE_AETHER_SYMBOL),
          setOf(EVIDENCE_RESEARCH_NOTE, "evidence_alt_elias_diary")
        ),
        optionalEvidenceIds = listOf(EVIDENCE_SILVER_CUTTING),
        misleadingEvidenceIds = listOf("evidence_misleading_gas_leak_rumor")
      )
    )

    return manager
  }

  /**
   * Creates the primary Phase 4 scenario for the Restricted Wing Sealing investigation.
   */
  fun createRestrictedWingScenario(): InvestigationScenario {
    return InvestigationScenario(
      scenarioId = SCENARIO_RESTRICTED_WING_SEALING,
      title = "The Sealing of the Restricted Wing",
      description = "Reconstruct how and why the Academy Restricted Wing was locked during the evacuation, and configure the alchemical sealing mechanism to enter.",
      requiredFacts = listOf(FACT_RESTRICTED_WING_SEALED),
      evidencePool = listOf(
        EVIDENCE_WARD_PROTOCOL_NOTE,
        EVIDENCE_SEALING_DIAGRAM,
        EVIDENCE_PEDESTAL_INSCRIPTION,
        EVIDENCE_ATRIUM_LEDGER
      ),
      minimumEvidenceRequired = 2,
      validEvidenceCombinations = listOf(
        setOf(EVIDENCE_WARD_PROTOCOL_NOTE, EVIDENCE_PEDESTAL_INSCRIPTION),
        setOf(EVIDENCE_WARD_PROTOCOL_NOTE, EVIDENCE_ATRIUM_LEDGER),
        setOf(EVIDENCE_SEALING_DIAGRAM, EVIDENCE_PEDESTAL_INSCRIPTION),
        setOf(EVIDENCE_SEALING_DIAGRAM, EVIDENCE_ATRIUM_LEDGER)
      ),
      misleadingEvidence = listOf(EVIDENCE_OBSOLETE_DRAFT),
      puzzleId = "puzzle_restricted_wing_seal"
    )
  }
}
