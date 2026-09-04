package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.story.MemoryFragment

/**
 * Atmospheric, non-intrusive memory echo overlay.
 * Renders an ethereal Aether vignette and transcribed memory dialogues
 * while preserving full player first-person movement and camera look controls.
 */
@Composable
fun MemoryEchoOverlay(
  activeMemory: MemoryFragment,
  playbackElapsed: Float,
  onConcludeMemory: () -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "aether_memory_pulse")
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.25f,
    targetValue = 0.55f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_alpha"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("memory_echo_overlay")
  ) {
    // 1. Ethereal Aether Edge Vignette (Cyan to Deep Violet)
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          brush = Brush.radialGradient(
            colors = listOf(
              Color.Transparent,
              Color(0x2218FFFF).copy(alpha = pulseAlpha * 0.4f),
              Color(0x664A148C).copy(alpha = pulseAlpha * 0.8f)
            )
          )
        )
    )

    // Glowing border frame
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp)
        .border(
          width = 1.5.dp,
          brush = Brush.horizontalGradient(
            colors = listOf(Color(0xFF64FFDA), Color(0xFFB388FF), Color(0xFF64FFDA))
          ),
          shape = RoundedCornerShape(12.dp)
        )
    )

    // 2. Top Header: Memory Indicator & Title
    Column(
      modifier = Modifier
        .align(Alignment.TopCenter)
        .padding(top = 18.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Surface(
        color = Color(0xD8100C1D),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64FFDA).copy(alpha = 0.7f))
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(Color(0xFF64FFDA))
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "AETHER MEMORY ECHO",
            color = Color(0xFF64FFDA),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = activeMemory.title,
        color = Color(0xFFF3E5F5),
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Serif
      )
    }

    // 3. Center/Bottom: Transcribed Dialogues & Revelations
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(bottom = 60.dp, start = 40.dp, end = 40.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Show dialogues whose trigger delay has elapsed
      activeMemory.dialogueLines.forEach { line ->
        AnimatedVisibility(
          visible = playbackElapsed >= line.delaySeconds,
          enter = fadeIn(tween(400)),
          exit = fadeOut(tween(200))
        ) {
          Surface(
            color = Color(0xCC0E0B18),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF7C4DFF).copy(alpha = 0.6f)),
            modifier = Modifier
              .padding(vertical = 4.dp)
              .fillMaxWidth(0.85f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${line.speaker}: ",
                color = Color(0xFF80D8FF),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = "\"${line.line}\"",
                color = Color(0xFFEDE7F6),
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic
              )
            }
          }
        }
      }

      // Revelation Statement
      if (playbackElapsed >= 4.0f) {
        Surface(
          color = Color(0xDD2A1438),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFD54F).copy(alpha = 0.8f)),
          modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth(0.85f)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "TRUTH: ",
              color = Color(0xFFFFD54F),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
            Text(
              text = activeMemory.revelationStatement,
              color = Color(0xFFFFF8E1),
              fontSize = 12.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Button to conclude/integrate early
      Button(
        onClick = onConcludeMemory,
        colors = ButtonDefaults.buttonColors(
          containerColor = Color(0xD0311B92),
          contentColor = Color(0xFF80D8FF)
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF64FFDA).copy(alpha = 0.6f)),
        modifier = Modifier.testTag("conclude_memory_button")
      ) {
        Text(
          text = "INTEGRATE MEMORY ECHO",
          fontSize = 11.sp,
          letterSpacing = 1.sp,
          fontWeight = FontWeight.SemiBold
        )
      }
    }
  }
}
