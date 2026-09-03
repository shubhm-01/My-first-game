package com.example.core

import com.example.magic.AbilityType

/**
 * High-level mode of the game application.
 */
enum class GameMode {
  EXPLORING,
  INSPECTING,
  JOURNAL,
  INVENTORY,
  PAUSED
}

/**
 * Tracks coarse, persistent progression flags across the academy.
 */
data class WorldState(
  val currentRoomId: String = "academy_entrance_hall",
  val unlockedPassages: Set<String> = emptySet(),
  val solvedPuzzles: Set<String> = emptySet(),
  val discoveredClues: Set<String> = emptySet(),
  val unlockedAbilities: Set<AbilityType> = emptySet()
)

/**
 * Master game state container holding player and world session data.
 */
data class GameState(
  val mode: GameMode = GameMode.EXPLORING,
  val worldState: WorldState = WorldState(),
  val activeSaveSlot: Int = 1
)

/**
 * Immutable snapshot of the overall game state, designed for save serialization.
 */
data class GameSnapshot(
  val saveSlotId: Int = 1,
  val timestampEpoch: Long = System.currentTimeMillis(),
  val gameMode: GameMode = GameMode.EXPLORING,
  val worldState: WorldState = WorldState()
)
