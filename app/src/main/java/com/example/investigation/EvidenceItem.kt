package com.example.investigation

/**
 * Standardized categorization of alchemical and environmental evidence.
 */
enum class EvidenceType(val label: String) {
  DOCUMENT("Document"),
  OBJECT("Relic / Object"),
  INSCRIPTION("Inscription"),
  SYMBOL("Aether Sigil"),
  MEMORY("Memory Resonance"),
  EXPERIMENT("Experiment"),
  ENVIRONMENTAL("Environmental"),
  AUDIO("Acoustic Echo"),
  OTHER("Curiosity")
}

/**
 * Significance tier for clues and evidence in investigation deduction.
 */
enum class EvidenceImportance {
  CRITICAL,
  SIGNIFICANT,
  CORROBORATING,
  OPTIONAL,
  MISLEADING
}

/**
 * Categorization for journal organization and thematic investigation tracking.
 */
enum class JournalCategory(val displayName: String) {
  PEOPLE("People"),
  PLACES("Places"),
  ALCHEMY("Alchemy"),
  INCIDENT("The Incident"),
  AETHER("Aether"),
  MEMORIES("Memories"),
  UNKNOWN("Uncategorized")
}

/**
 * Reusable, data-driven representation of a discoverable piece of evidence.
 * Supports variable narrative placement, multi-fact correlations, and replayable pools.
 */
data class EvidenceItem(
  val id: String,
  val title: String,
  val description: String,
  val type: EvidenceType,
  val importance: EvidenceImportance = EvidenceImportance.SIGNIFICANT,
  val category: JournalCategory = JournalCategory.UNKNOWN,
  val relatedFactIds: List<String> = emptyList(),
  val relatedCharacters: List<String> = emptyList(),
  val relatedLocations: List<String> = emptyList(),
  val metadata: Map<String, String> = emptyMap(),
  val isDiscovered: Boolean = false,
  val discoveredTimestamp: Long = 0L
)
