package com.example.ui

import com.example.core.GameMode

/**
 * Coarse UI state consumed by Jetpack Compose overlays.
 * Never receives high-frequency tick updates, preventing recomposition spikes.
 */
data class GameUiState(
  val gameMode: GameMode = GameMode.EXPLORING,
  val currentRoomName: String = "Academy Atrium",
  val interactionPrompt: String? = null,
  val statusBanner: String? = null,
  val isControlsEnabled: Boolean = true
)
