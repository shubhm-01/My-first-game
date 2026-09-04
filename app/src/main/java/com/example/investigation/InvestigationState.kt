package com.example.investigation

/**
 * Immutable snapshot of the player's active investigation progress.
 * Decoupled from Compose and Filament rendering lifecycles.
 */
data class InvestigationState(
  val discoveredEvidenceIds: Set<String> = emptySet(),
  val discoveredFactIds: Set<String> = emptySet(),
  val examinedObjectIds: Set<String> = emptySet(),
  val totalCatalogEvidenceCount: Int = 0,
  val totalCatalogFactCount: Int = 0,
  val progressFraction: Float = 0.0f
)
