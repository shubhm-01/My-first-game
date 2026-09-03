package com.example.puzzle

import com.example.data.SaveableEntity

/**
 * State lifecycle of a puzzle node.
 */
enum class PuzzleStatus {
  LOCKED,
  ACTIVE,
  SOLVED,
  FAILED
}

/**
 * Common interface for modular, reusable puzzle components.
 */
interface PuzzleNode : SaveableEntity {
  val puzzleId: String
  val status: PuzzleStatus

  /**
   * Evaluates if current conditions satisfy the puzzle requirement.
   */
  fun evaluate(): Boolean

  /**
   * Resets puzzle to initial state after failure.
   */
  fun reset()
}
