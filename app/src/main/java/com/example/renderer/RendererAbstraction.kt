package com.example.renderer

import com.example.player.Position3D
import com.example.player.Rotation2D

/**
 * Definition of a 3D environment scene or room.
 */
data class SceneDefinition(
  val sceneId: String,
  val displayName: String,
  val modelAssetPath: String,
  val spawnPosition: Position3D = Position3D(0f, 1.7f, 0f),
  val ambientLightColor: Int = 0xFF2B2533.toInt(), // Warm mystical dungeon ambient
  val mainLightIntensity: Float = 50000.0f
)

/**
 * High-level abstraction for 3D world rendering.
 * Keeps game logic, player controller, and puzzle code completely decoupled from the rendering backend.
 */
interface WorldRenderer {
  /**
   * Initializes renderer contexts and buffers.
   */
  fun initialize()

  /**
   * Loads a given scene or room into the active viewport.
   */
  fun loadScene(scene: SceneDefinition, onLoaded: () -> Unit = {})

  /**
   * Updates camera viewpoint from player movement/look state.
   */
  fun updateCamera(position: Position3D, rotation: Rotation2D)

  /**
   * Applies an interaction highlight or shader pulse on an in-world object.
   */
  fun highlightEntity(entityId: String?, isHighlighted: Boolean)

  /**
   * Frees native GPU resources when navigating away or destroying the view.
   */
  fun destroy()
}
