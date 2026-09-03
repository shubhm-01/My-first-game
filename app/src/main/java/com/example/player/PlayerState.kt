package com.example.player

import com.example.magic.AbilityType

/**
 * 3D spatial coordinate for player positioning.
 */
data class Position3D(
  val x: Float = 0.0f,
  val y: Float = 1.7f, // Average eye level in meters
  val z: Float = 0.0f
)

/**
 * Camera rotation angles in degrees.
 */
data class Rotation2D(
  val yaw: Float = 0.0f,   // Horizontal look (left/right)
  val pitch: Float = 0.0f  // Vertical look (up/down, clamped between -85° and 85°)
)

/**
 * Encapsulates the player's active physical and operational state.
 */
data class PlayerState(
  val position: Position3D = Position3D(),
  val rotation: Rotation2D = Rotation2D(),
  val isMoving: Boolean = false,
  val activeAbility: AbilityType? = null,
  val movementSpeed: Float = 2.4f // Standard exploration walk speed in m/s
)
