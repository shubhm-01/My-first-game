package com.example.story

import com.example.data.SaveableEntity

/**
 * Dialogue or atmospheric utterance preserved within an Aether memory echo.
 */
data class MemoryDialogue(
  val speaker: String,
  val line: String,
  val delaySeconds: Float = 0.0f
)

/**
 * Policy governing whether a memory can be replayed after its initial manifestation.
 */
enum class MemoryReplayPolicy {
  PLAY_ONCE,
  REPLAYABLE_VIA_JOURNAL
}

/**
 * Trigger condition types for memory fragments.
 */
sealed class MemoryTriggerCondition {
  data class OnObjectExamined(val entityId: String) : MemoryTriggerCondition()
  data class OnEvidenceDiscovered(val evidenceId: String) : MemoryTriggerCondition()
  data class OnLocationEntered(val locationId: String) : MemoryTriggerCondition()
  data class OnFactUnlocked(val factId: String) : MemoryTriggerCondition()
  data class OnFlagTrue(val flagName: String) : MemoryTriggerCondition()
}

/**
 * Encapsulates an Aether-preserved memory sequence.
 * Replays past emotional or sensory events preserved in crystallized Aether.
 */
data class MemoryFragment(
  val id: String,
  val title: String,
  val associatedLocationId: String,
  val associatedFactIds: List<String>,
  val triggerCondition: MemoryTriggerCondition,
  val sensoryEcho: String,
  val revelationStatement: String,
  val dialogueLines: List<MemoryDialogue>,
  val durationSeconds: Float = 9.0f,
  val replayPolicy: MemoryReplayPolicy = MemoryReplayPolicy.REPLAYABLE_VIA_JOURNAL,
  val variantTag: String = "primary",
  var isDiscovered: Boolean = false,
  var isTriggered: Boolean = false,
  var discoveredTimestamp: Long = 0L
) : SaveableEntity {

  override val entityId: String get() = "memory_$id"

  override fun captureState(): Map<String, String> {
    return mapOf(
      "id" to id,
      "isDiscovered" to isDiscovered.toString(),
      "isTriggered" to isTriggered.toString(),
      "discoveredTimestamp" to discoveredTimestamp.toString()
    )
  }

  override fun restoreState(state: Map<String, String>) {
    isDiscovered = state["isDiscovered"]?.toBooleanStrictOrNull() ?: isDiscovered
    isTriggered = state["isTriggered"]?.toBooleanStrictOrNull() ?: isTriggered
    discoveredTimestamp = state["discoveredTimestamp"]?.toLongOrNull() ?: discoveredTimestamp
  }
}
