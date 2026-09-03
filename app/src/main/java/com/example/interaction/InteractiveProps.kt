package com.example.interaction

import com.example.collision.CollisionSystem
import com.example.core.GameEvent
import com.example.player.Position3D

/**
 * State of a physical doorway in the academy.
 */
enum class DoorState {
  CLOSED,
  OPEN
}

/**
 * Interactive Academy Door prop with persistent-friendly state, toggleable collision,
 * and appropriate domain events (DoorOpened, DoorClosed).
 */
class AcademyDoor(
  override val entityId: String = "door_atrium_gate",
  override val displayName: String = "Academy Sanctum Door",
  override val position: Position3D = Position3D(0.0f, 1.6f, -5.9f),
  override val boundingRadius: Float = 1.55f,
  var collisionSystem: CollisionSystem? = null,
  initialState: DoorState = DoorState.CLOSED
) : SpatialInteractable {

  var state: DoorState = initialState
    private set

  override val interactionType: InteractionType = InteractionType.DOOR

  override val primaryAction: ActionVerb
    get() = if (state == DoorState.CLOSED) ActionVerb.OPEN else ActionVerb.CLOSE

  override val actionPrompt: String
    get() = when (state) {
      DoorState.CLOSED -> "OPEN: $displayName"
      DoorState.OPEN -> "CLOSE: $displayName"
    }

  override val isInteractive: Boolean = true

  init {
    collisionSystem?.setDoorBarrier(state == DoorState.OPEN)
  }

  override fun captureState(): Map<String, String> {
    return mapOf(
      "state" to state.name,
      "entityId" to entityId
    )
  }

  override fun restoreState(state: Map<String, String>) {
    state["state"]?.let {
      try {
        this.state = DoorState.valueOf(it)
        collisionSystem?.setDoorBarrier(this.state == DoorState.OPEN)
      } catch (ignored: IllegalArgumentException) {}
    }
  }

  override fun onInteract(): GameEvent {
    return if (state == DoorState.CLOSED) {
      state = DoorState.OPEN
      collisionSystem?.setDoorBarrier(true)
      GameEvent.DoorOpened(doorId = entityId)
    } else {
      state = DoorState.CLOSED
      collisionSystem?.setDoorBarrier(false)
      GameEvent.DoorClosed(doorId = entityId)
    }
  }
}

/**
 * Interactive Alchemy Table test prop with inspection description.
 */
class AlchemyTable(
  override val entityId: String = "prop_alchemy_table",
  override val displayName: String = "Alchemy Workbench",
  override val position: Position3D = Position3D(0.0f, 0.85f, -3.5f),
  override val boundingRadius: Float = 1.25f,
  val description: String = "An abandoned alchemy table. Several dried reagents remain scattered across its surface."
) : SpatialInteractable {

  override val interactionType: InteractionType = InteractionType.EXAMINE
  override val primaryAction: ActionVerb = ActionVerb.EXAMINE
  override val actionPrompt: String = "EXAMINE: $displayName"
  override val isInteractive: Boolean = true

  private var examinedCount = 0

  override fun captureState(): Map<String, String> = mapOf(
    "examinedCount" to examinedCount.toString(),
    "entityId" to entityId
  )

  override fun restoreState(state: Map<String, String>) {
    examinedCount = state["examinedCount"]?.toIntOrNull() ?: 0
  }

  override fun onInteract(): GameEvent {
    examinedCount++
    return GameEvent.ObjectExamined(
      entityId = entityId,
      description = description
    )
  }
}

/**
 * State of an in-world collectible object.
 */
enum class CollectibleState {
  PRESENT,
  COLLECTED
}

/**
 * Interactive Collectible ingredient prop with TAKE action, state persistence,
 * and ObjectCollected / ItemAcquired domain events.
 */
class CollectibleIngredient(
  override val entityId: String = "ingredient_silver_leaf",
  override val displayName: String = "Silver Herb Cutting",
  override val position: Position3D = Position3D(0.7f, 0.95f, -3.2f),
  override val boundingRadius: Float = 0.55f,
  initialState: CollectibleState = CollectibleState.PRESENT
) : SpatialInteractable {

  var state: CollectibleState = initialState
    private set

  override val interactionType: InteractionType = InteractionType.PICKUP
  override val primaryAction: ActionVerb = ActionVerb.TAKE
  override val actionPrompt: String = "TAKE: $displayName"

  override val isInteractive: Boolean
    get() = state == CollectibleState.PRESENT

  override fun canInteract(): Boolean = isInteractive

  override fun captureState(): Map<String, String> = mapOf(
    "state" to state.name,
    "entityId" to entityId
  )

  override fun restoreState(state: Map<String, String>) {
    state["state"]?.let {
      try {
        this.state = CollectibleState.valueOf(it)
      } catch (ignored: IllegalArgumentException) {}
    }
  }

  override fun onInteract(): GameEvent? {
    if (state != CollectibleState.PRESENT) return null
    state = CollectibleState.COLLECTED
    return GameEvent.ObjectCollected(
      entityId = entityId,
      itemName = displayName
    )
  }
}
