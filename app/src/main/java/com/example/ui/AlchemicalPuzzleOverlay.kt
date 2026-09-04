package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioController
import com.example.audio.SoundEffect
import com.example.core.GameEventBus
import com.example.investigation.InvestigationManager
import com.example.puzzle.AlchemicalSealingPuzzle
import com.example.puzzle.AlchemicalSymbol
import com.example.puzzle.PuzzleStatus

@Composable
fun AlchemicalPuzzleOverlay(
  puzzle: AlchemicalSealingPuzzle,
  investigationManager: InvestigationManager,
  eventBus: GameEventBus? = null,
  audioController: AudioController? = null,
  onClose: () -> Unit,
  onPuzzleSolved: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  // Local state for UI updates
  var revision by remember { mutableIntStateOf(0) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var feedbackIsError by remember { mutableStateOf(false) }

  val isSolved = puzzle.status == PuzzleStatus.SOLVED
  val activeConfig = puzzle.activeConfig
  val hasDiscoveredClue = investigationManager.isEvidenceDiscovered(activeConfig.clueEvidenceId)

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xE60D0A14)) // Atmospheric dark vignette
      .clickable(enabled = false) {}
      .padding(16.dp),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = Color(0xFF181424),
      border = androidx.compose.foundation.BorderStroke(
        width = 1.5.dp,
        brush = Brush.verticalGradient(
          colors = if (isSolved) listOf(Color(0xFF81C784), Color(0xFF388E3C))
          else listOf(Color(0xFFE2C48D), Color(0xFF7D5260))
        )
      ),
      shadowElevation = 24.dp,
      modifier = Modifier
        .widthIn(max = 680.dp)
        .fillMaxWidth()
        .testTag("puzzle_dialog_surface")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // 1. Header Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (isSolved) Color(0xFF2E7D32) else Color(0xFF3B2E4A)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (isSolved) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = "Lock Status",
                tint = if (isSolved) Color(0xFFA5D6A7) else Color(0xFFE2C48D),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "RESTRICTED WING SEAL",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF3E5F5),
                letterSpacing = 1.sp
              )
              Text(
                text = "Tripartite Alchemical Ward",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB39DDB)
              )
            }
          }

          IconButton(
            onClick = onClose,
            modifier = Modifier
              .size(40.dp)
              .testTag("puzzle_close_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Puzzle",
              tint = Color(0xFFB0A7BA)
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Investigation Clue Callout
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF241C30))
            .border(1.dp, if (hasDiscoveredClue) Color(0x66E2C48D) else Color(0x33B39DDB), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = if (hasDiscoveredClue) "EVIDENCE CORROBORATION:" else "INVESTIGATION HINT:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (hasDiscoveredClue) Color(0xFFE2C48D) else Color(0xFFB39DDB)
              )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = if (hasDiscoveredClue) {
                val item = investigationManager.getEvidence(activeConfig.clueEvidenceId)
                "\"${item?.title ?: "Directive"}\": ${item?.description?.take(130)}..."
              } else {
                "The sealing ward requires harmonic balancing of three alchemical principles. Search the workbench, lectern, and academy documents to uncover the required alignment."
              },
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFFD1C4E9),
              lineHeight = 16.sp
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 3. Three Interactive Tumbler Columns
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically
        ) {
          val labels = activeConfig.slotLabels
          for (slotIndex in 0 until 3) {
            val label = labels.getOrNull(slotIndex) ?: "Ward ${slotIndex + 1}"
            val selectedSymbol = puzzle.currentSlots.getOrNull(slotIndex)

            TumblerColumn(
              slotIndex = slotIndex,
              label = label,
              symbol = selectedSymbol,
              isSolved = isSolved,
              onCycleUp = {
                if (!isSolved) {
                  puzzle.cycleSlot(slotIndex, forward = false)
                  audioController?.playSfx(SoundEffect.UI_CLICK, 0.4f)
                  feedbackMessage = null
                  revision++
                }
              },
              onCycleDown = {
                if (!isSolved) {
                  puzzle.cycleSlot(slotIndex, forward = true)
                  audioController?.playSfx(SoundEffect.UI_CLICK, 0.4f)
                  feedbackMessage = null
                  revision++
                }
              }
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Subtle Player Feedback Banner
        AnimatedVisibility(
          visible = feedbackMessage != null || isSolved,
          enter = fadeIn(),
          exit = fadeOut()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(
                if (isSolved) Color(0x334CAF50)
                else if (feedbackIsError) Color(0x33FF7043)
                else Color(0x33B39DDB)
              )
              .border(
                1.dp,
                if (isSolved) Color(0xFF81C784)
                else if (feedbackIsError) Color(0xFFFF8A65)
                else Color(0xFFB39DDB),
                RoundedCornerShape(8.dp)
              )
              .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = if (isSolved) "WARD HARMONIZED — RESTRICTED WING UNLOCKED"
              else feedbackMessage ?: "",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = if (isSolved) Color(0xFFA5D6A7) else if (feedbackIsError) Color(0xFFFFCCBC) else Color(0xFFE1BEE7),
              textAlign = TextAlign.Center
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Action Control Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = {
              if (!isSolved) {
                puzzle.resetWithBus(eventBus)
                audioController?.playSfx(SoundEffect.DOOR_CLOSE, 0.5f)
                feedbackMessage = "Mechanism reset to neutral alignment."
                feedbackIsError = false
                revision++
              }
            },
            enabled = !isSolved,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("puzzle_reset_button"),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = Color(0xFFCE93D8)
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("RESET", fontWeight = FontWeight.SemiBold)
          }

          Button(
            onClick = {
              if (isSolved) {
                onClose()
              } else {
                val success = puzzle.evaluateWithBus(eventBus)
                if (success) {
                  audioController?.playSfx(SoundEffect.MYSTERY_CHIME, 1.0f)
                  feedbackMessage = "The three principles harmonize. The lock mechanism clunks open!"
                  feedbackIsError = false
                  onPuzzleSolved()
                } else {
                  audioController?.playSfx(SoundEffect.DOOR_CLOSE, 0.4f)
                  feedbackMessage = "Resonance misaligned. The mechanism clicks shut."
                  feedbackIsError = true
                }
                revision++
              }
            },
            modifier = Modifier
              .weight(1.5f)
              .height(48.dp)
              .testTag("puzzle_engage_button"),
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isSolved) Color(0xFF2E7D32) else Color(0xFF8E24AA),
              contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text(
              text = if (isSolved) "PROCEED (UNLOCKED)" else "ENGAGE SEAL",
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun TumblerColumn(
  slotIndex: Int,
  label: String,
  symbol: AlchemicalSymbol?,
  isSolved: Boolean,
  onCycleUp: () -> Unit,
  onCycleDown: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.width(135.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(
      text = label.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = Color(0xFFB0A7BA),
      fontSize = 11.sp,
      letterSpacing = 0.5.sp,
      maxLines = 1
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Up Button
    IconButton(
      onClick = onCycleUp,
      enabled = !isSolved,
      modifier = Modifier
        .size(44.dp)
        .testTag("slot_${slotIndex}_up")
    ) {
      Icon(
        imageVector = Icons.Default.KeyboardArrowUp,
        contentDescription = "Cycle $label Up",
        tint = if (!isSolved) Color(0xFFE2C48D) else Color.Gray,
        modifier = Modifier.size(28.dp)
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Symbol Display Card
    Box(
      modifier = Modifier
        .size(105.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF13101C))
        .border(
          width = 2.dp,
          color = if (symbol != null) Color(symbol.elementColorHex) else Color(0xFF3F3550),
          shape = RoundedCornerShape(16.dp)
        )
        .clickable(enabled = !isSolved) { onCycleDown() }
        .testTag("slot_${slotIndex}_card"),
      contentAlignment = Alignment.Center
    ) {
      if (symbol != null) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
          modifier = Modifier.padding(6.dp)
        ) {
          Text(
            text = symbol.glyph,
            fontSize = 34.sp,
            color = Color(symbol.elementColorHex),
            fontFamily = FontFamily.Serif
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = symbol.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF3E5F5)
          )
          Text(
            text = symbol.principle.split("/").firstOrNull()?.trim() ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFB0A7BA),
            fontSize = 9.sp,
            maxLines = 1
          )
        }
      } else {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(
            text = "—",
            fontSize = 32.sp,
            color = Color(0xFF756885)
          )
          Text(
            text = "TAP TO SET",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8E829E),
            fontSize = 10.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Down Button
    IconButton(
      onClick = onCycleDown,
      enabled = !isSolved,
      modifier = Modifier
        .size(44.dp)
        .testTag("slot_${slotIndex}_down")
    ) {
      Icon(
        imageVector = Icons.Default.KeyboardArrowDown,
        contentDescription = "Cycle $label Down",
        tint = if (!isSolved) Color(0xFFE2C48D) else Color.Gray,
        modifier = Modifier.size(28.dp)
      )
    }
  }
}
