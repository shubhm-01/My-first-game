package com.example.data

/**
 * Universal interface for game entities that possess persistent state (chests, switches, puzzles, doors).
 * Ensures any future game entity can save/restore without re-architecting the database.
 */
interface SaveableEntity {
  val entityId: String

  /**
   * Captures entity-specific key-value state for storage.
   */
  fun captureState(): Map<String, String>

  /**
   * Restores entity-specific state from storage.
   */
  fun restoreState(state: Map<String, String>)
}
