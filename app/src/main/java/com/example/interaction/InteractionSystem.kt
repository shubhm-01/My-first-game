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
  override val isInteractive: Boolean = true,
  override val evidenceId: String? = null
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
  val maxInteractionDistance: Float = 3.8f,
  val collisionSystem: CollisionSystem? = null
) {
  val interactables = mutableListOf<SpatialInteractable>()

  // Dedicated test objects
  val door = AcademyDoor(collisionSystem = collisionSystem)
  val alchemyTable = AlchemyTable()
  val collectibleIngredient = CollectibleIngredient()
  val sealingMechanism = SealingMechanism()

  init {
    // 1. Academy Door (OPEN / CLOSE test interactable)
    registerInteractable(door)

    // 2. Alchemy Table (EXAMINE test interactable)
    registerInteractable(alchemyTable)

    // 3. Collectible Object (TAKE test interactable)
    registerInteractable(collectibleIngredient)

    // 4. Ancient Bronze Cauldron (EXPERIMENT evidence)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_cauldron",
        displayName = "Ancient Bronze Cauldron",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Ancient Cauldron",
        position = Position3D(-2.2f, 0.45f, -0.5f),
        boundingRadius = 0.85f,
        interactionMessage = "Cold ash and faint lavender residue remain inside the heavy bronze cauldron.",
        evidenceId = "evidence_ash_residue"
      )
    )

    // 5. Mystic Aether Crystal Shard (AETHER SYMBOL evidence)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_crystal",
        displayName = "Luminous Aether Shard",
        interactionType = InteractionType.MECHANISM,
        actionPrompt = "EXAMINE: Aether Crystal",
        position = Position3D(2.2f, 0.85f, -0.5f),
        boundingRadius = 0.75f,
        interactionMessage = "The aether crystal hums with a quiet, soothing magical resonance.",
        evidenceId = "evidence_aether_symbol"
      )
    )

    // 6. Evacuation Ledger Fragment (INCIDENT DOCUMENT evidence)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_atrium_ledger",
        displayName = "Evacuation Ledger",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Evacuation Ledger",
        position = Position3D(-1.8f, 0.95f, -3.8f),
        boundingRadius = 0.65f,
        interactionMessage = "An official academy registry dated to the night the grand halls were sealed.",
        evidenceId = "evidence_atrium_ledger"
      )
    )

    // 7. Everflame Wall Torch
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

    // 8. Phase 4: Alchemical Sealing Mechanism beside the door
    registerInteractable(sealingMechanism)

    // 9. Phase 4: Archival Sealing Diagram (East Lectern)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_sealing_diagram",
        displayName = "Archival Sealing Diagram",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Sealing Diagram",
        position = Position3D(2.4f, 0.9f, -3.2f),
        boundingRadius = 0.75f,
        interactionMessage = "A technical schematic outlining alternate harmonic sequences for sealing the gateway.",
        evidenceId = "evidence_sealing_diagram"
      )
    )

    // 10. Phase 4: Flame Binding Directive (Workbench Drawer)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_ward_directive",
        displayName = "Flame Binding Directive",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Ward Directive",
        position = Position3D(0.9f, 0.85f, -3.5f),
        boundingRadius = 0.65f,
        interactionMessage = "Master Elias's emergency order detailing the three-principle flame ward sequence.",
        evidenceId = "evidence_ward_protocol_note"
      )
    )

    // 11. Phase 4: Discarded Apprentice Draft (Fair Misleading Evidence)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_apprentice_draft",
        displayName = "Discarded Apprentice Draft",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Discarded Draft",
        position = Position3D(-2.8f, 1.1f, -2.5f),
        boundingRadius = 0.65f,
        interactionMessage = "A rejected draft from an apprentice with Master Elias's red corrections.",
        evidenceId = "evidence_obsolete_draft"
      )
    )

    // 12. Phase 5: Overturned Research Desk (West Research Bay)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_sealed_research_desk",
        displayName = "Overturned Research Desk",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Council Directive",
        position = Position3D(-3.5f, 0.9f, -8.5f),
        boundingRadius = 0.85f,
        interactionMessage = "An emergency order stamped with wax seals of the High Council commanding the immediate quarantine and entombment of the lower levels.",
        evidenceId = "evidence_sealed_research_log"
      )
    )

    // 13. Phase 5: Shattered Distillation Apparatus (West Research Bay)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_shattered_apparatus",
        displayName = "Shattered Distillation Column",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Scorched Notes",
        position = Position3D(-4.2f, 0.6f, -10.0f),
        boundingRadius = 0.75f,
        interactionMessage = "Calcified glass fragments surround a burned notebook warning that Elias's living crystal resonance was intentionally buried.",
        evidenceId = "evidence_burnt_lab_journal"
      )
    )

    // 14. Phase 5: Containment Registry Shelf (East Archive)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_archive_shelf",
        displayName = "Containment Registry Shelf",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Security Log",
        position = Position3D(3.8f, 1.1f, -7.5f),
        boundingRadius = 0.85f,
        interactionMessage = "Registry quill logs confirming the outer perimeter gates were barred by remote Council order.",
        evidenceId = "evidence_emergency_security_log"
      )
    )

    // 15. Phase 5: Resonating Aether Core (East Archive - Triggers Memory Echo)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_resonating_aether_relic",
        displayName = "Resonating Aether Core",
        interactionType = InteractionType.MECHANISM,
        actionPrompt = "ATTUNE: Memory Echo",
        position = Position3D(3.5f, 1.0f, -9.8f),
        boundingRadius = 0.85f,
        interactionMessage = "The hovering crystalline sphere vibrates with concentrated memory resonance. An auditory echo begins to play!",
        evidenceId = "evidence_aether_resonance_relic"
      )
    )

    // 16. Phase 5: Containment Warning Plaque (Subterranean Gate)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_warning_inscription",
        displayName = "Containment Warning Plaque",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "READ: Quarantine Warning",
        position = Position3D(-1.2f, 1.8f, -13.6f),
        boundingRadius = 0.75f,
        interactionMessage = "A heavy bronze plaque warning of absolute permanent quarantine for the lower subterranean levels.",
        evidenceId = "evidence_scribe_warning_plaque"
      )
    )

    // 17. Phase 5: Subterranean Sanctum Gate (Corridor North End)
    registerInteractable(
      SceneInteractable(
        entityId = "prop_deep_wing_iron_gate",
        displayName = "Subterranean Sanctum Gate",
        interactionType = InteractionType.EXAMINE,
        actionPrompt = "EXAMINE: Warded Portcullis",
        position = Position3D(0.0f, 1.5f, -13.9f),
        boundingRadius = 0.9f,
        interactionMessage = "Massive iron portcullis barred tight. Faint crystalline Aether light pulses slowly from the darkness beyond."
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

