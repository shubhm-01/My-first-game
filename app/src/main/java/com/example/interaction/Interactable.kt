package com.example.interaction

import com.example.core.GameEvent
import com.example.data.SaveableEntity

/**
 * Categorization of physical interactions in the academy.
 */
enum class InteractionType {
  EXAMINE,
  PICKUP,
  SWITCH,
  DOOR,
  CONTAINER,
  MECHANISM,
  ALCHEMY_STATION
}

/**
 * Standardized verbs for contextual action buttons.
 * Supports current core actions (EXAMINE, TAKE, OPEN, CLOSE, ACTIVATE)
 * while leaving architecture open for future actions (USE_ITEM, CAST_SPELL, SOLVE_PUZZLE, BREW, TALK).
 */
enum class ActionVerb(val label: String) {
  EXAMINE("EXAMINE"),
  TAKE("TAKE"),
  OPEN("OPEN"),
  CLOSE("CLOSE"),
  ACTIVATE("ACTIVATE"),
  // Extensible future slots (not yet activated)
  USE_ITEM("USE ITEM"),
  CAST_SPELL("CAST SPELL"),
  SOLVE_PUZZLE("SOLVE PUZZLE"),
  BREW("BREW"),
  TALK("TALK")
}

/**
 * Base contract for any in-world object that can be focused and manipulated by the player.
 */
interface Interactable : SaveableEntity {
  override val entityId: String
  val displayName: String
  val interactionType: InteractionType
  val actionPrompt: String // Contextual prompt displayed near reticle e.g. "OPEN: Academy Door"
  val primaryAction: ActionVerb get() = when (interactionType) {
    InteractionType.DOOR -> ActionVerb.OPEN
    InteractionType.PICKUP -> ActionVerb.TAKE
    InteractionType.SWITCH, InteractionType.MECHANISM -> ActionVerb.ACTIVATE
    else -> ActionVerb.EXAMINE
  }
  val isInteractive: Boolean

  /**
   * Available contextual actions for this interactable (extensible for multi-action targets).
   */
  fun getAvailableActions(): List<ActionVerb> = listOf(primaryAction)

  /**
   * Evaluates if conditions are met for interaction (e.g. key in hand, correct spell active).
   */
  fun canInteract(): Boolean = isInteractive

  /**
   * Executes the interaction and returns an optional domain event.
   */
  fun onInteract(): GameEvent?
}

