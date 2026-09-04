package com.example.story

import com.example.player.Position3D

/**
 * Data-driven representation of distinct architectural areas in the Academy.
 * Enables reusable spatial triggers, story anchors, and memory associations.
 */
data class StoryLocation(
  val id: String,
  val displayName: String,
  val description: String,
  val minX: Float,
  val maxX: Float,
  val minZ: Float,
  val maxZ: Float
) {
  fun contains(x: Float, z: Float): Boolean {
    return x in minX..maxX && z in minZ..maxZ
  }

  fun contains(pos: Position3D): Boolean = contains(pos.x, pos.z)

  companion object {
    const val LOCATION_ATRIUM = "loc_academy_atrium"
    const val LOCATION_RESTRICTED_ENTRANCE = "loc_restricted_entrance"
    const val LOCATION_RESTRICTED_CORRIDOR = "loc_restricted_corridor"
    const val LOCATION_RESEARCH_BAY = "loc_research_bay"
    const val LOCATION_RESTRICTED_ARCHIVE = "loc_restricted_archive"
    const val LOCATION_DEEP_GATE = "loc_deep_gate"

    /**
     * Default spatial taxonomy for Chapter 0 (Atrium) and Chapter 1 (Restricted Wing).
     */
    val ACADEMY_LOCATIONS = listOf(
      StoryLocation(
        id = LOCATION_ATRIUM,
        displayName = "Academy Atrium",
        description = "The central vaulted reception hall, bearing remnants of sudden evacuation.",
        minX = -5.0f, maxX = 5.0f,
        minZ = -6.0f, maxZ = 6.0f
      ),
      StoryLocation(
        id = LOCATION_RESTRICTED_ENTRANCE,
        displayName = "Restricted Wing Threshold",
        description = "The reinforced heavy stone doorway and alchemical sealing mechanism.",
        minX = -1.5f, maxX = 1.5f,
        minZ = -5.8f, maxZ = -5.2f
      ),
      StoryLocation(
        id = LOCATION_RESTRICTED_CORRIDOR,
        displayName = "Restricted Wing Corridor",
        description = "A chilling stone passage flanked by heavy containment archways.",
        minX = -1.6f, maxX = 1.6f,
        minZ = -12.8f, maxZ = -5.9f
      ),
      StoryLocation(
        id = LOCATION_RESEARCH_BAY,
        displayName = "Abandoned Research Bay",
        description = "An experimental workshop strewn with shattered distillation columns and scorched records.",
        minX = -5.6f, maxX = -1.4f,
        minZ = -11.5f, maxZ = -6.5f
      ),
      StoryLocation(
        id = LOCATION_RESTRICTED_ARCHIVE,
        displayName = "Containment Records Archive",
        description = "A quiet repository of sealed records, locked storage presses, and an active Aether relic.",
        minX = 1.4f, maxX = 5.6f,
        minZ = -11.5f, maxZ = -6.5f
      ),
      StoryLocation(
        id = LOCATION_DEEP_GATE,
        displayName = "Subterranean Sanctum Gate",
        description = "A massive iron portcullis humming with residual Aether wards leading deeper into the Academy.",
        minX = -2.0f, maxX = 2.0f,
        minZ = -14.2f, maxZ = -12.8f
      )
    )

    val ACADEMY_ATRIUM: StoryLocation get() = ACADEMY_LOCATIONS[0]
    val RESTRICTED_WING_ENTRANCE: StoryLocation get() = ACADEMY_LOCATIONS[1]
    val RESTRICTED_WING_CORRIDOR: StoryLocation get() = ACADEMY_LOCATIONS[2]
    val ABANDONED_RESEARCH_BAY: StoryLocation get() = ACADEMY_LOCATIONS[3]
    val CONTAINMENT_RECORDS_ARCHIVE: StoryLocation get() = ACADEMY_LOCATIONS[4]
    val DEEP_SUBTERRANEAN_GATE: StoryLocation get() = ACADEMY_LOCATIONS[5]

    fun fromPosition(x: Float, z: Float): StoryLocation? = findLocationForPosition(x, z)

    fun findLocationForPosition(x: Float, z: Float): StoryLocation? {
      // Prioritize smaller/more specific zones first (rooms and thresholds before generic atrium)
      return ACADEMY_LOCATIONS.firstOrNull { it.id != LOCATION_ATRIUM && it.contains(x, z) }
        ?: ACADEMY_LOCATIONS.firstOrNull { it.contains(x, z) }
    }
  }
}
