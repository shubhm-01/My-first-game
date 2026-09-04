package com.example.story

/**
 * High-level narrative progression stages.
 */
enum class StoryChapter(
  val id: String,
  val title: String,
  val subtitle: String,
  val primaryObjective: String
) {
  CHAPTER_0_ATRIUM(
    id = "ch_0_atrium",
    title = "Chapter 0: The Empty Sanctum",
    subtitle = "Academy Atrium",
    primaryObjective = "Inspect the surroundings and breach the Restricted Wing ward."
  ),
  CHAPTER_1_RESTRICTED_WING(
    id = "ch_1_restricted_wing",
    title = "Chapter 1: The Deliberate Seal",
    subtitle = "Restricted Wing",
    primaryObjective = "Investigate the abandoned research chambers and reconstruct the sealing event."
  ),
  CHAPTER_2_ARCHIVES(
    id = "ch_2_archives",
    title = "Chapter 2: Echoes in the Dust",
    subtitle = "Grand Archives",
    primaryObjective = "Uncover Elias's primary research and the first harmonic resonance."
  ),
  CHAPTER_3_UNDERGROUND_LAB(
    id = "ch_3_underground_lab",
    title = "Chapter 3: The Subterranean Hearth",
    subtitle = "Deep Laboratory",
    primaryObjective = "Navigate the lower quarantine chambers toward the central core."
  );

  companion object {
    val CHAPTER_0_AWAKENING: StoryChapter get() = CHAPTER_0_ATRIUM
    val CHAPTER_1_REVELATION: StoryChapter get() = CHAPTER_1_RESTRICTED_WING

    fun fromId(id: String): StoryChapter = entries.firstOrNull { it.id == id } ?: CHAPTER_0_ATRIUM
  }
}
