package com.example.interaction

import com.example.collision.CollisionSystem
import com.example.core.GameEvent
import com.example.player.Position3D
import kotlin.math.sqrt

/**
 * Result of an interaction raycast from the player's view center.
 */
data class RaycastHit(
  val interactable: Interactable,
  val distance: Float,
  val prompt: String,
  val actionVerb: ActionVerb = interactable.primaryAction
)

/**
 * Interface combining [Interactable] with spatial position and bounding sphere for raycasting.
 */
interface SpatialInteractable : Interactable {
  val position: Position3D
  val boundingRadius: Float
}

/**
 * Generic implementation of [SpatialInteractable] for scene props.
 */
class SceneInteractable(
  override val entityId: String,
  override val displayName: String,
  override val interactionType: InteractionType,
  override val actionPrompt: String,
  override val position: Position3D,
  override val boundingRadius: Float = 0.8f,
  val interactionMessage: String = "Interacted with $displayName",
  override val isInteractive: Boolean = true
) : SpatialInteractable {

  private var stateStore = mutableMapOf<String, String>()

  override fun captureState(): Map<String, String> = stateStore.toMap()

  override fun restoreState(state: Map<String, String>) {
    stateStore.clear()
    stateStore.putAll(state)
  }

  override fun onInteract(): GameEvent {
    return GameEvent.InteractionTriggered(
      targetId = entityId,
      action = interactionMessage
    )
  }
}

/**
 * Manages interactable targets in the test chamber and performs real-time center-screen raycasts.
 * Reusable for future puzzles, inventory, alchemy, and magic systems.
 */
class InteractionSystem(
  val maxInteractionDistance: Float = 3.2f,
  val collisionSystem: CollisionSystem? = null
) {
  val interactables = mutableListOf<SpatialInteractable>()

  // Dedicated test objects
  val door = AcademyDoor(collisionSystem = collisionSystem)
  val alchemyTable = AlchemyTable()
  val collectibleIngredient = CollectibleIngredient()

  init {
    // 1. Academy Door (OPEN / CLOSE test interactable)
    registerInteractable(door)

    // 2. Alchemy Table (EXAMINE test interactable)
    registerInteractable(alchemyTable)

    // 3. Collectible Object (TAKE test interactable)
    registerInteractable(collectibleIngredient)

    // 4. Ancient Bronze Cauldron
    registerInteractable(
      SceneInteractable(
        entityId = "prop_cauldron",
        displayName = "Ancient Bronze Cauldron",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Ancient Cauldron",
        position = Position3D(-2.2f, 0.45f, -0.5f),
        boundingRadius = 0.85f,
        interactionMessage = "Cold ash and faint lavender residue remain inside the heavy bronze cauldron."
      )
    )

    // 5. Mystic Aether Crystal Shard
    registerInteractable(
      SceneInteractable(
        entityId = "prop_crystal",
        displayName = "Luminous Aether Shard",
        interactionType = InteractionType.MECHANISM,
        actionPrompt = "EXAMINE: Aether Crystal",
        position = Position3D(2.2f, 0.85f, -0.5f),
        boundingRadius = 0.75f,
        interactionMessage = "The aether crystal hums with a quiet, soothing magical resonance."
      )
    )

    // 6. Everflame Wall Torch
    registerInteractable(
      SceneInteractable(
        entityId = "prop_torch_west",
        displayName = "Everflame Sconce",
        interactionType = InteractionType.SWITCH,
        actionPrompt = "EXAMINE: Everflame Torch",
        position = Position3D(-4.7f, 2.0f, -1.0f),
        boundingRadius = 0.7f,
        interactionMessage = "An undying alchemical flame cast in deep golden warmth."
      )
    )
  }

  fun registerInteractable(target: SpatialInteractable) {
    if (!interactables.any { it.entityId == target.entityId }) {
      interactables.add(target)
    }
  }

  fun unregisterInteractable(entityId: String) {
    interactables.removeAll { it.entityId == entityId }
  }

  fun findInteractable(entityId: String): SpatialInteractable? {
    return interactables.find { it.entityId == entityId }
  }

  /**
   * Evaluates center-screen raycast against scene interactables.
   * [rayOrigin] is the eye position of the camera.
   * [rayDir] is the normalized (dirX, dirY, dirZ) direction vector.
   * Returns the closest valid [RaycastHit], or null if none targeted.
   */
  fun performRaycast(
    rayOrigin: Position3D,
    rayDir: Triple<Float, Float, Float>
  ): RaycastHit? {
    val (dx, dy, dz) = rayDir
    var closestHit: RaycastHit? = null
    var closestDist = maxInteractionDistance

    for (target in interactables) {
      if (!target.canInteract()) continue

      val vx = target.position.x - rayOrigin.x
      val vy = target.position.y - rayOrigin.y
      val vz = target.position.z - rayOrigin.z

      // Dot product: projection of vector to target along ray direction
      val t = vx * dx + vy * dy + vz * dz
      if (t <= 0.0f || t > closestDist) continue

      // Closest point on ray to target center
      val projX = rayOrigin.x + t * dx
      val projY = rayOrigin.y + t * dy
      val projZ = rayOrigin.z + t * dz

      val distSq = (target.position.x - projX) * (target.position.x - projX) +
        (target.position.y - projY) * (target.position.y - projY) +
        (target.position.z - projZ) * (target.position.z - projZ)

      val r = target.boundingRadius
      if (distSq <= r * r) {
        val actualDist = sqrt(
          (target.position.x - rayOrigin.x) * (target.position.x - rayOrigin.x) +
            (target.position.y - rayOrigin.y) * (target.position.y - rayOrigin.y) +
            (target.position.z - rayOrigin.z) * (target.position.z - rayOrigin.z)
        )

        if (actualDist < closestDist) {
          closestDist = actualDist
          closestHit = RaycastHit(
            interactable = target,
            distance = actualDist,
            prompt = target.actionPrompt,
            actionVerb = target.primaryAction
          )
        }
      }
    }

    return closestHit
  }
}

