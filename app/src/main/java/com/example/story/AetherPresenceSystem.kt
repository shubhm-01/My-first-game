package com.example.story

import com.example.audio.AudioController
import com.example.audio.SoundEffect
import com.example.core.GameEvent
import com.example.core.GameEventBus
import com.example.player.Position3D
import kotlin.math.sqrt

/**
 * Manages subtle, atmospheric Aether phenomena in the environment.
 * Delivers eerie psychological cues (faint humming, sudden ozone chill, vibrating stone)
 * without revealing the full Aether consciousness prematurely.
 */
class AetherPresenceSystem(
  private val audioController: AudioController? = null,
  private val eventBus: GameEventBus? = null
) {

  private val minIntervalMs: Long = 14000L // At least 14 seconds between subtle cues
  private var lastPhenomenonTime: Long = -minIntervalMs

  // Spatial Aether manifestation hot-spots
  private val deepGateNode = Position3D(0.0f, 1.5f, -13.6f)
  private val archiveRelicNode = Position3D(3.5f, 1.0f, -9.8f)

  fun update(playerPosition: Position3D, currentTimeMs: Long = System.currentTimeMillis()): String? {
    if (currentTimeMs - lastPhenomenonTime < minIntervalMs) return null

    val distToGate = distance(playerPosition, deepGateNode)
    val distToRelic = distance(playerPosition, archiveRelicNode)

    if (distToGate < 2.4f) {
      lastPhenomenonTime = currentTimeMs
      audioController?.playSfx(SoundEffect.AETHER_HUM, 0.45f)
      audioController?.playSfx(SoundEffect.METALLIC_DISTANT_CLANG, 0.25f)
      val message = "A low crystalline vibration hums through the iron portcullis... cold ozone chills the air."
      eventBus?.emit(GameEvent.AetherPresenceDetected("prop_deep_wing_iron_gate", message))
      return message
    } else if (distToRelic < 2.0f) {
      lastPhenomenonTime = currentTimeMs
      audioController?.playSfx(SoundEffect.AETHER_HUM, 0.5f)
      val message = "The crystalline core glows faintly as you draw near, vibrating with suppressed memory."
      eventBus?.emit(GameEvent.AetherPresenceDetected("prop_resonating_aether_relic", message))
      return message
    }

    return null
  }

  private fun distance(p1: Position3D, p2: Position3D): Float {
    val dx = p1.x - p2.x
    val dy = p1.y - p2.y
    val dz = p1.z - p2.z
    return sqrt(dx * dx + dy * dy + dz * dz)
  }
}
