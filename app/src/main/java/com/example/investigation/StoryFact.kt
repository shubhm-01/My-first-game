package com.example.investigation

/**
 * Data-driven representation of a core story truth or deduction.
 * Story facts remain locked until the player discovers sufficient corroborating evidence.
 */
data class StoryFact(
  val id: String,
  val title: String,
  val statement: String,
  val category: JournalCategory,
  val contributingEvidenceIds: Set<String> = emptySet(),
  val requiredEvidenceCount: Int = 1,
  val isUnlocked: Boolean = false,
  val unlockedTimestamp: Long = 0L,
  val tags: List<String> = emptyList()
) {
  /**
   * Evaluates if the current set of discovered evidence IDs fulfills this fact's requirements.
   */
  fun canUnlock(discoveredEvidenceIds: Set<String>): Boolean {
    val matchedCount = contributingEvidenceIds.count { it in discoveredEvidenceIds }
    return matchedCount >= requiredEvidenceCount
  }

  /**
   * Returns current completion fraction [0.0f..1.0f] toward unlocking this truth.
   */
  fun progressFraction(discoveredEvidenceIds: Set<String>): Float {
    if (requiredEvidenceCount <= 0) return 1.0f
    val matchedCount = contributingEvidenceIds.count { it in discoveredEvidenceIds }
    return (matchedCount.toFloat() / requiredEvidenceCount).coerceIn(0.0f, 1.0f)
  }
}
