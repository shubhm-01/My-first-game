package com.example.story

/**
 * Journal entry recovered from the academy's past.
 */
data class JournalEntry(
  val id: String,
  val chapter: Int,
  val title: String,
  val content: String,
  val clueSummary: String? = null
)

/**
 * Narrative progression tracker contract.
 */
interface StoryProgressTracker {
  fun isFlagRaised(flagKey: String): Boolean
  fun setFlag(flagKey: String, value: Boolean)
  fun getUnlockedJournals(): List<JournalEntry>
}
