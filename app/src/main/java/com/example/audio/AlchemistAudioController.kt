package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Lightweight, zero-asset procedural audio controller for ambient subterranean drone
 * and interactive sound effects.
 */
class AlchemistAudioController(
  private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : AudioController {

  private var ambientTrackInstance: AudioTrack? = null
  private var ambientJob: Job? = null
  private var isPlayingAmbient: Boolean = false

  override fun playAmbience(track: AmbientTrack) {
    if (isPlayingAmbient) return
    isPlayingAmbient = true

    ambientJob = scope.launch {
      try {
        val sampleRate = 22050
        val bufferSize = AudioTrack.getMinBufferSize(
          sampleRate,
          AudioFormat.CHANNEL_OUT_MONO,
          AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(sampleRate / 2)

        ambientTrackInstance = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(bufferSize)
          .setTransferMode(AudioTrack.MODE_STREAM)
          .build()

        ambientTrackInstance?.play()

        // Generate warm, dark subterranean resonant hum (55Hz fundamental + 110Hz sub-harmonic)
        val shortBuffer = ShortArray(1024)
        var phase1 = 0.0
        var phase2 = 0.0
        val freq1 = 55.0
        val freq2 = 110.0

        while (isActive && isPlayingAmbient) {
          for (i in shortBuffer.indices) {
            val sampleVal = (sin(phase1) * 0.15 + sin(phase2) * 0.08) * Short.MAX_VALUE
            shortBuffer[i] = sampleVal.toInt().toShort()
            phase1 = (phase1 + 2.0 * Math.PI * freq1 / sampleRate) % (2.0 * Math.PI)
            phase2 = (phase2 + 2.0 * Math.PI * freq2 / sampleRate) % (2.0 * Math.PI)
          }
          ambientTrackInstance?.write(shortBuffer, 0, shortBuffer.size)
        }
      } catch (e: Throwable) {
        // Fallback gracefully on devices without audio hardware support
      }
    }
  }

  override fun stopAmbience() {
    isPlayingAmbient = false
    ambientJob?.cancel()
    ambientJob = null
    try {
      ambientTrackInstance?.stop()
      ambientTrackInstance?.release()
    } catch (ignored: Throwable) {}
    ambientTrackInstance = null
  }

  override fun playSfx(sfx: SoundEffect, volume: Float) {
    scope.launch {
      try {
        val sampleRate = 22050
        when (sfx) {
          SoundEffect.FOOTSTEP_STONE -> {
            // Low stone tap (60ms decay)
            playProceduralBurst(sampleRate, 0.06f, 130.0f, volume * 0.35f)
          }
          SoundEffect.MYSTERY_CHIME -> {
            // High resonant mystical chime (300ms)
            playProceduralChime(sampleRate, 0.35f, 880.0f, volume * 0.4f)
          }
          SoundEffect.UI_CLICK -> {
            playProceduralBurst(sampleRate, 0.02f, 600.0f, volume * 0.25f)
          }
          SoundEffect.DOOR_OPEN, SoundEffect.STONE_DOOR_SLIDE -> {
            // Deep resonant stone sliding friction sound (450ms frequency glide from 90Hz to 160Hz)
            playProceduralSlide(sampleRate, 0.45f, 90.0f, 160.0f, volume * 0.55f)
          }
          SoundEffect.DOOR_CLOSE -> {
            // Heavy stone gate sealing impact (300ms low thud with rumble)
            playProceduralSlide(sampleRate, 0.35f, 180.0f, 75.0f, volume * 0.6f)
          }
          SoundEffect.ITEM_PICKUP -> {
            // Crystal pickup chime (two rapid upward harmonic notes 520Hz -> 780Hz)
            playProceduralPickup(sampleRate, 0.22f, volume * 0.5f)
          }
          SoundEffect.AETHER_HUM -> {
            // Shimmering 220Hz + 330Hz harmonic hum
            playProceduralDualTone(sampleRate, 0.6f, 220.0f, 330.0f, volume * 0.4f)
          }
          SoundEffect.METALLIC_DISTANT_CLANG -> {
            // Distant heavy metallic gate impact with slow reverb tail
            playProceduralDualTone(sampleRate, 0.7f, 98.0f, 294.0f, volume * 0.5f)
          }
          SoundEffect.MEMORY_RESONANCE -> {
            // Ethereal crystalline chord (A major: 440Hz -> 554Hz -> 659Hz)
            playProceduralMemoryResonance(sampleRate, 0.8f, volume * 0.6f)
          }
          SoundEffect.CONTAINMENT_ALERT -> {
            // Emergency containment alert pulse
            playProceduralDualTone(sampleRate, 0.35f, 587.0f, 440.0f, volume * 0.45f)
          }
          SoundEffect.FAINT_VOICE_WHISPER -> {
            // Filtered breath whisper burst
            playProceduralBurst(sampleRate, 0.25f, 180.0f, volume * 0.2f)
          }
          else -> {
            playProceduralBurst(sampleRate, 0.04f, 220.0f, volume * 0.2f)
          }
        }
      } catch (ignored: Throwable) {}
    }
  }

  private fun playProceduralBurst(sampleRate: Int, durationSec: Float, baseFreq: Float, volume: Float) {
    val numSamples = (sampleRate * durationSec).toInt()
    val buffer = ShortArray(numSamples)
    var phase = 0.0

    for (i in 0 until numSamples) {
      val decay = 1.0f - (i.toFloat() / numSamples)
      val sample = (sin(phase) * decay * volume * Short.MAX_VALUE).toInt().toShort()
      buffer[i] = sample
      phase += 2.0 * Math.PI * baseFreq / sampleRate
    }

    playShortAudioBuffer(sampleRate, buffer)
  }

  private fun playProceduralChime(sampleRate: Int, durationSec: Float, freq: Float, volume: Float) {
    val numSamples = (sampleRate * durationSec).toInt()
    val buffer = ShortArray(numSamples)
    var phase1 = 0.0
    var phase2 = 0.0

    for (i in 0 until numSamples) {
      val progress = i.toFloat() / numSamples
      val decay = kotlin.math.exp(-progress * 5.0).toFloat()
      val s1 = sin(phase1) * 0.7f
      val s2 = sin(phase2) * 0.3f
      val sample = ((s1 + s2) * decay * volume * Short.MAX_VALUE).toInt().toShort()
      buffer[i] = sample
      phase1 += 2.0 * Math.PI * freq / sampleRate
      phase2 += 2.0 * Math.PI * (freq * 1.5) / sampleRate
    }

    playShortAudioBuffer(sampleRate, buffer)
  }

  private fun playProceduralSlide(sampleRate: Int, durationSec: Float, startFreq: Float, endFreq: Float, volume: Float) {
    val numSamples = (sampleRate * durationSec).toInt()
    val buffer = ShortArray(numSamples)
    var phase = 0.0

    for (i in 0 until numSamples) {
      val progress = i.toFloat() / numSamples
      val currentFreq = startFreq + (endFreq - startFreq) * progress
      // Fade in at start and fade out at end to avoid pops
      val envelope = sin(progress * Math.PI.toFloat())
      val sample = (sin(phase) * envelope * volume * Short.MAX_VALUE).toInt().toShort()
      buffer[i] = sample
      phase += 2.0 * Math.PI * currentFreq / sampleRate
    }

    playShortAudioBuffer(sampleRate, buffer)
  }

  private fun playProceduralPickup(sampleRate: Int, durationSec: Float, volume: Float) {
    val numSamples = (sampleRate * durationSec).toInt()
    val buffer = ShortArray(numSamples)
    var phase1 = 0.0
    var phase2 = 0.0
    val half = numSamples / 2

    for (i in 0 until numSamples) {
      val freq = if (i < half) 523.25f else 783.99f // C5 -> G5 upward chime
      val segProgress = if (i < half) i.toFloat() / half else (i - half).toFloat() / half
      val decay = kotlin.math.exp(-segProgress * 4.0).toFloat()
      val sample = ((sin(phase1) * 0.75f + sin(phase2) * 0.25f) * decay * volume * Short.MAX_VALUE).toInt().toShort()
      buffer[i] = sample
      phase1 += 2.0 * Math.PI * freq / sampleRate
      phase2 += 2.0 * Math.PI * (freq * 2.0f) / sampleRate
    }

    playShortAudioBuffer(sampleRate, buffer)
  }

  private fun playProceduralDualTone(sampleRate: Int, durationSec: Float, freq1: Float, freq2: Float, volume: Float) {
    val numSamples = (sampleRate * durationSec).toInt()
    val buffer = ShortArray(numSamples)
    var phase1 = 0.0
    var phase2 = 0.0

    for (i in 0 until numSamples) {
      val progress = i.toFloat() / numSamples
      val envelope = sin(progress * Math.PI.toFloat()) * kotlin.math.exp(-progress * 2.0).toFloat()
      val sample = ((sin(phase1) * 0.6f + sin(phase2) * 0.4f) * envelope * volume * Short.MAX_VALUE).toInt().toShort()
      buffer[i] = sample
      phase1 += 2.0 * Math.PI * freq1 / sampleRate
      phase2 += 2.0 * Math.PI * freq2 / sampleRate
    }
    playShortAudioBuffer(sampleRate, buffer)
  }

  private fun playProceduralMemoryResonance(sampleRate: Int, durationSec: Float, volume: Float) {
    val numSamples = (sampleRate * durationSec).toInt()
    val buffer = ShortArray(numSamples)
    var phase1 = 0.0
    var phase2 = 0.0
    var phase3 = 0.0
    // Ethereal chord: 440Hz (A4), 554.37Hz (C#5), 659.25Hz (E5)
    val freq1 = 440.0
    val freq2 = 554.37
    val freq3 = 659.25

    for (i in 0 until numSamples) {
      val progress = i.toFloat() / numSamples
      // Gentle attack followed by shimmering slow decay
      val attack = (progress * 5.0f).coerceAtMost(1.0f)
      val decay = kotlin.math.exp(-progress * 2.5).toFloat()
      val shimmer = 1.0f + 0.15f * sin(progress * 20.0 * Math.PI).toFloat()
      val sample = ((sin(phase1) * 0.45 + sin(phase2) * 0.35 + sin(phase3) * 0.2) * attack * decay * shimmer * volume * Short.MAX_VALUE).toInt().toShort()
      buffer[i] = sample
      phase1 += 2.0 * Math.PI * freq1 / sampleRate
      phase2 += 2.0 * Math.PI * freq2 / sampleRate
      phase3 += 2.0 * Math.PI * freq3 / sampleRate
    }
    playShortAudioBuffer(sampleRate, buffer)
  }

  private fun playShortAudioBuffer(sampleRate: Int, buffer: ShortArray) {
    val track = AudioTrack.Builder()
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      .setAudioFormat(
        AudioFormat.Builder()
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setSampleRate(sampleRate)
          .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
          .build()
      )
      .setBufferSizeInBytes(buffer.size * 2)
      .setTransferMode(AudioTrack.MODE_STATIC)
      .build()

    track.write(buffer, 0, buffer.size)
    track.play()
    track.setNotificationMarkerPosition(buffer.size)
    track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
      override fun onMarkerReached(t: AudioTrack?) {
        try {
          t?.stop()
          t?.release()
        } catch (ignored: Throwable) {}
      }
      override fun onPeriodicNotification(track: AudioTrack?) {}
    })
  }
}
