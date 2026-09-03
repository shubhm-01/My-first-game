package com.example.renderer

import com.example.player.Position3D
import com.example.player.Rotation2D

/**
 * Concrete implementation of [WorldRenderer] connecting the decoupled game simulation
 * to the SceneView 3D viewport.
 */
class AcademyWorldRenderer : WorldRenderer {

  private var sceneHolder: AcademySceneHolder? = null
  private var currentSceneDef: SceneDefinition? = null
  private var isInitialized = false

  override fun initialize() {
    isInitialized = true
  }

  fun attachSceneHolder(holder: AcademySceneHolder) {
    this.sceneHolder = holder
  }

  override fun loadScene(scene: SceneDefinition, onLoaded: () -> Unit) {
    currentSceneDef = scene
    onLoaded()
  }

  override fun updateCamera(position: Position3D, rotation: Rotation2D) {
    sceneHolder?.updateCamera(
      x = position.x,
      y = position.y,
      z = position.z,
      pitch = rotation.pitch,
      yaw = rotation.yaw
    )
  }

  override fun highlightEntity(entityId: String?, isHighlighted: Boolean) {
    // Phase 1 stub: prepares for Phase 2 highlight shaders and outlines
  }

  fun onDoorStateChanged(isOpen: Boolean) {
    sceneHolder?.setDoorOpen(isOpen)
  }

  fun onCollectibleStateChanged(isVisible: Boolean) {
    sceneHolder?.setCollectibleVisible(isVisible)
  }

  override fun destroy() {
    sceneHolder = null
    isInitialized = false
  }

  fun getLoadedAssetCount(): Int = sceneHolder?.loadedModelCount ?: 0
}
