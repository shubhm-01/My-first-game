package com.example.investigation

/**
 * Reusable architectural foundation for variable and replayable investigation pools.
 * Guarantees fair, data-driven narrative randomization:
 * - Specifies what fact must be deducible
 * - Maintains a candidate pool of varying evidence that can establish it
 * - Validates that any sampled subset in a playthrough is mathematically solvable
 * - Accommodates optional lore and fair misleading clues
 */
data class VariableEvidencePool(
  val poolId: String,
  val requiredFactId: String,
  val possibleEvidenceIds: List<String>,
  val minimumEvidenceCount: Int = 2,
  val validCombinations: List<Set<String>> = emptyList(),
  val optionalEvidenceIds: List<String> = emptyList(),
  val misleadingEvidenceIds: List<String> = emptyList()
) {
  /**
   * Validates whether a given subset of available in-world evidence guarantees solvability.
   * Prevents impossible seed generations or narrative dead-ends.
   */
  fun validateSolvability(availableEvidence: Set<String>): Boolean {
    // If explicit valid combinations are declared, at least one complete combination must be present
    if (validCombinations.isNotEmpty()) {
      return validCombinations.any { combo -> combo.all { it in availableEvidence } }
    }

    // Otherwise, the number of intersecting valid clues must meet or exceed minimumEvidenceCount
    val matchingCount = possibleEvidenceIds.count { it in availableEvidence }
    return matchingCount >= minimumEvidenceCount
  }

  /**
   * Samples a valid, solvable subset of evidence IDs for a dynamic playthrough seed.
   */
  fun sampleSolvableConfiguration(
    rngSeed: Long = System.currentTimeMillis()
  ): Set<String> {
    val random = java.util.Random(rngSeed)
    if (validCombinations.isNotEmpty()) {
      val chosenCombo = validCombinations[random.nextInt(validCombinations.size)]
      val result = chosenCombo.toMutableSet()
      // Optionally add random optional clue if available
      if (optionalEvidenceIds.isNotEmpty() && random.nextBoolean()) {
        result.add(optionalEvidenceIds[random.nextInt(optionalEvidenceIds.size)])
      }
      return result
    }

    val shuffled = possibleEvidenceIds.shuffled(random)
    val takeCount = minimumEvidenceCount.coerceAtMost(shuffled.size)
    val result = shuffled.take(takeCount).toMutableSet()

    if (optionalEvidenceIds.isNotEmpty() && random.nextBoolean()) {
      result.add(optionalEvidenceIds[random.nextInt(optionalEvidenceIds.size)])
    }
    return result
  }
}
