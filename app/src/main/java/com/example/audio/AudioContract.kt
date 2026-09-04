package com.example.audio

/**
 * Sound effects taxonomy for the underground academy.
 */
enum class SoundEffect {
  FOOTSTEP_STONE,
  STONE_DOOR_SLIDE,
  DOOR_OPEN,
  DOOR_CLOSE,
  ITEM_PICKUP,
  LEVER_ACTIVATE,
  CAULDRON_BUBBLE,
  SPELL_IGNITE,
  SPELL_FROST,
  MYSTERY_CHIME,
  PUZZLE_SOLVE,
  UI_CLICK,
  AETHER_HUM,
  METALLIC_DISTANT_CLANG,
  MEMORY_RESONANCE,
  CONTAINMENT_ALERT,
  FAINT_VOICE_WHISPER
}

/**
 * Atmospheric background audio cues.
 */
enum class AmbientTrack {
  SUBTERRANEAN_ECHO,
  ALCHEMY_LAB_HUM,
  ANCIENT_ARCHIVE_WHISPER
}

/**
 * Audio manager contract.
 */
interface AudioController {
  fun playSfx(sfx: SoundEffect, volume: Float = 1.0f)
  fun playAmbience(track: AmbientTrack)
  fun stopAmbience()
}
