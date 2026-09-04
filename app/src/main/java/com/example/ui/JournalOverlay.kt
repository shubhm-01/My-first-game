package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.investigation.EvidenceImportance
import com.example.investigation.EvidenceItem
import com.example.investigation.InvestigationManager
import com.example.investigation.JournalCategory
import com.example.investigation.StoryFact
import com.example.story.MemoryFragment
import com.example.story.MemoryManager

/**
 * Landscape-optimized Investigation Journal & Evidence Viewer.
 * Fits the dark atmospheric psychological mystery aesthetic of THE LAST ALCHEMIST.
 */
@Composable
fun JournalOverlay(
  investigationManager: InvestigationManager,
  memoryManager: MemoryManager? = null,
  selectedEvidenceId: String? = null,
  onReplayMemory: ((String) -> Unit)? = null,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  val discoveredEvidence = remember(investigationManager.getDiscoveredCount()) {
    investigationManager.getDiscoveredEvidence()
  }
  val allFacts = remember(investigationManager.getUnlockedFactCount()) {
    investigationManager.getAllStoryFactsWithStatus()
  }
  val discoveredMemories = remember(memoryManager?.getDiscoveredMemories()?.size) {
    memoryManager?.getDiscoveredMemories() ?: emptyList()
  }

  // Active view tab
  var activeTab by remember { mutableStateOf(JournalTab.EVIDENCE) }
  var selectedCategory by remember { mutableStateOf<JournalCategory?>(null) }
  var inspectedEvidenceId by remember {
    mutableStateOf(selectedEvidenceId ?: discoveredEvidence.firstOrNull()?.id)
  }

  val filteredEvidence = remember(selectedCategory, discoveredEvidence) {
    if (selectedCategory == null) {
      discoveredEvidence
    } else {
      discoveredEvidence.filter { it.category == selectedCategory }
    }
  }

  val activeItem = remember(inspectedEvidenceId, discoveredEvidence) {
    discoveredEvidence.find { it.id == inspectedEvidenceId } ?: filteredEvidence.firstOrNull()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xDC0A0810))
      .padding(16.dp)
      .testTag("journal_overlay"),
    contentAlignment = Alignment.Center
  ) {
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .border(1.dp, Color(0xFF423752), RoundedCornerShape(16.dp)),
      color = Color(0xF413101B),
      shape = RoundedCornerShape(16.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(horizontal = 20.dp, vertical = 12.dp)
      ) {
        // 1. Header Bar: Title, Progress, and Close
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "INVESTIGATION JOURNAL",
              color = Color(0xFFE2C48D),
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 2.sp,
              fontFamily = FontFamily.Serif
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(top = 2.dp)
            ) {
              val progress = investigationManager.getProgressFraction()
              Text(
                text = "Reconstruction: ${(progress * 100).toInt()}% • ${discoveredEvidence.size} Clues Discovered • ${investigationManager.getUnlockedFactCount()} Truths Established",
                color = Color(0xFFA69E93),
                fontSize = 11.sp
              )
              Spacer(modifier = Modifier.width(12.dp))
              LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                  .width(100.dp)
                  .height(4.dp)
                  .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFFD4A359),
                trackColor = Color(0xFF2C2438)
              )
            }
          }

          // Top Mode Switcher (Clues vs Story Facts)
          Row(
            modifier = Modifier
              .background(Color(0xFF211A2C), RoundedCornerShape(8.dp))
              .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            JournalTabButton(
              title = "EVIDENCE (${discoveredEvidence.size})",
              isSelected = activeTab == JournalTab.EVIDENCE,
              onClick = { activeTab = JournalTab.EVIDENCE }
            )
            JournalTabButton(
              title = "RECONSTRUCTED TRUTHS (${investigationManager.getUnlockedFactCount()})",
              isSelected = activeTab == JournalTab.STORY_FACTS,
              onClick = { activeTab = JournalTab.STORY_FACTS }
            )
            JournalTabButton(
              title = "MEMORIES (${discoveredMemories.size})",
              isSelected = activeTab == JournalTab.MEMORIES,
              onClick = { activeTab = JournalTab.MEMORIES }
            )
          }

          Spacer(modifier = Modifier.width(16.dp))

          // Dismiss Button (min 48dp touch target)
          IconButton(
            onClick = onClose,
            modifier = Modifier
              .size(48.dp)
              .testTag("journal_close_button")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Close Journal",
              tint = Color(0xFFD4A359)
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Main Body Content
        when (activeTab) {
          JournalTab.EVIDENCE -> {
            // Category Filter Row
            LazyRow(
              modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              item {
                CategoryChip(
                  label = "ALL (${discoveredEvidence.size})",
                  isSelected = selectedCategory == null,
                  onClick = { selectedCategory = null }
                )
              }
              items(JournalCategory.values()) { category ->
                val count = discoveredEvidence.count { it.category == category }
                CategoryChip(
                  label = "${category.displayName} ($count)",
                  isSelected = selectedCategory == category,
                  onClick = { selectedCategory = category }
                )
              }
            }

            // Split View: Evidence List (Left) and Evidence Detail Inspector (Right)
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
              horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
              // Left: Discovered Evidence Item List
              Surface(
                modifier = Modifier
                  .width(260.dp)
                  .fillMaxHeight(),
                color = Color(0xFF191522),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C243A))
              ) {
                if (filteredEvidence.isEmpty()) {
                  Box(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(16.dp),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "No clues discovered in this category yet.\n\nExplore the Academy and examine objects to record evidence.",
                      color = Color(0xFF888090),
                      fontSize = 11.sp,
                      lineHeight = 16.sp
                    )
                  }
                } else {
                  LazyColumn(
                    modifier = Modifier
                      .fillMaxSize()
                      .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    items(filteredEvidence) { item ->
                      EvidenceListItem(
                        item = item,
                        isSelected = item.id == activeItem?.id,
                        onClick = { inspectedEvidenceId = item.id }
                      )
                    }
                  }
                }
              }

              // Right: Evidence Detail Inspector
              Surface(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight(),
                color = Color(0xFF1B1724),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF382F48))
              ) {
                if (activeItem != null) {
                  EvidenceDetailView(
                    item = activeItem,
                    investigationManager = investigationManager
                  )
                } else {
                  Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                  ) {
                    Text(
                      text = "Select a discovered clue to inspect details and decipher correlations.",
                      color = Color(0xFF888090),
                      fontSize = 13.sp
                    )
                  }
                }
              }
            }
          }

          JournalTab.STORY_FACTS -> {
            // Reconstructed Truths View
            StoryFactsView(
              facts = allFacts,
              investigationManager = investigationManager
            )
          }

          JournalTab.MEMORIES -> {
            // Discovered Aether Memories View
            MemoriesJournalView(
              memories = discoveredMemories,
              onReplay = onReplayMemory
            )
          }
        }
      }
    }
  }
}

enum class JournalTab {
  EVIDENCE,
  STORY_FACTS,
  MEMORIES
}

@Composable
fun JournalTabButton(
  title: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(if (isSelected) Color(0xFF44355B) else Color.Transparent)
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Text(
      text = title,
      color = if (isSelected) Color(0xFFF3ECE0) else Color(0xFFA096A8),
      fontSize = 11.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
    )
  }
}

@Composable
fun CategoryChip(
  label: String,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) Color(0xFF4C3D63) else Color(0xFF1D1826),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isSelected) Color(0xFFD4A359) else Color(0xFF332942)
    ),
    modifier = Modifier.clickable(onClick = onClick)
  ) {
    Text(
      text = label,
      color = if (isSelected) Color(0xFFE2C48D) else Color(0xFF9E96A6),
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
    )
  }
}

@Composable
fun EvidenceListItem(
  item: EvidenceItem,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = if (isSelected) Color(0xFF342845) else Color(0xFF221C2C),
    border = androidx.compose.foundation.BorderStroke(
      1.dp,
      if (isSelected) Color(0xFFD4A359) else Color(0xFF2C243A)
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
  ) {
    Column(modifier = Modifier.padding(10.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .background(Color(0xFF493B5F), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            text = item.type.label.uppercase(),
            color = Color(0xFFD0BCFF),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.weight(1f))
        if (item.importance == EvidenceImportance.CRITICAL) {
          Text(
            text = "CRITICAL",
            color = Color(0xFFFFB74D),
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = item.title,
        color = if (isSelected) Color(0xFFF3ECE0) else Color(0xFFD5CEBF),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1
      )
    }
  }
}

@Composable
fun EvidenceDetailView(
  item: EvidenceItem,
  investigationManager: InvestigationManager
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(18.dp)
  ) {
    item {
      // Header: Type, Importance, Category
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .background(Color(0xFF493B5F), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = item.type.label.uppercase(),
            color = Color(0xFFD0BCFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Box(
          modifier = Modifier
            .background(Color(0xFF2E243D), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = item.category.displayName,
            color = Color(0xFFE2C48D),
            fontSize = 10.sp
          )
        }
        if (item.relatedLocations.isNotEmpty()) {
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Location: ${item.relatedLocations.joinToString()}",
            color = Color(0xFF9E96A6),
            fontSize = 10.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Title
      Text(
        text = item.title,
        color = Color(0xFFF3ECE0),
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.Serif
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Narrative Description Card (Parchment-style dark card)
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF231E2D),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F3550)),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = item.description,
          color = Color(0xFFE5DDD0),
          fontSize = 13.sp,
          lineHeight = 20.sp,
          fontFamily = FontFamily.Serif,
          modifier = Modifier.padding(14.dp)
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Corroborated Story Facts
      Text(
        text = "CORROBORATED TRUTHS",
        color = Color(0xFFD4A359),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      val relatedFacts = item.relatedFactIds.mapNotNull { factId ->
        investigationManager.storyFacts[factId]
      }

      if (relatedFacts.isEmpty()) {
        Text(
          text = "No specific overarching hypotheses linked to this curiosity yet.",
          color = Color(0xFF888090),
          fontSize = 11.sp
        )
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          relatedFacts.forEach { fact ->
            val isUnlocked = investigationManager.isFactUnlocked(fact.id)
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (isUnlocked) Color(0xFF263324) else Color(0xFF241F2B),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isUnlocked) Color(0xFF81C784) else Color(0xFF42374E)
              ),
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = fact.title,
                    color = if (isUnlocked) Color(0xFFA5D6A7) else Color(0xFFE0D8CA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                  )
                  Text(
                    text = if (isUnlocked) fact.statement else "Corroborating evidence in progress...",
                    color = if (isUnlocked) Color(0xFFC8E6C9) else Color(0xFF9E96A6),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                  modifier = Modifier
                    .background(
                      if (isUnlocked) Color(0xFF1B5E20) else Color(0xFF3E334D),
                      RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                  Text(
                    text = if (isUnlocked) "ESTABLISHED" else "REQUIRES CLUES",
                    color = if (isUnlocked) Color(0xFFC8E6C9) else Color(0xFFD0BCFF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun StoryFactsView(
  facts: List<StoryFact>,
  investigationManager: InvestigationManager
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Text(
        text = "RECONSTRUCTED STORY TRUTHS",
        color = Color(0xFFE2C48D),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Text(
        text = "Deductions established by corroborating physical and environmental clues throughout the Academy.",
        color = Color(0xFFA096A8),
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
      )
    }

    items(facts) { fact ->
      val isUnlocked = investigationManager.isFactUnlocked(fact.id)
      val discoveredIds = investigationManager.getDiscoveredEvidence().map { it.id }.toSet()
      val matchedCount = fact.contributingEvidenceIds.count { it in discoveredIds }
      val totalRequired = fact.requiredEvidenceCount

      Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isUnlocked) Color(0xFF1E281C) else Color(0xFF1E1A27),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (isUnlocked) Color(0xFF81C784) else Color(0xFF382F48)
        ),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = fact.title,
              color = if (isUnlocked) Color(0xFFA5D6A7) else Color(0xFFE0D8CA),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
              modifier = Modifier
                .background(
                  if (isUnlocked) Color(0xFF2E7D32) else Color(0xFF403452),
                  RoundedCornerShape(4.dp)
                )
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                text = if (isUnlocked) "UNLOCKED" else "$matchedCount / $totalRequired CLUES",
                color = if (isUnlocked) Color.White else Color(0xFFE2C48D),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = if (isUnlocked) {
              fact.statement
            } else {
              "The underlying truth remains obscured. Discover $totalRequired related clues to reconstruct this deduction."
            },
            color = if (isUnlocked) Color(0xFFC8E6C9) else Color(0xFF888090),
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontFamily = FontFamily.Serif
          )
        }
      }
    }
  }
}

@Composable
fun MemoriesJournalView(
  memories: List<MemoryFragment>,
  onReplay: ((String) -> Unit)? = null
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(8.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    item {
      Text(
        text = "DISCOVERED AETHER MEMORY ECHOES",
        color = Color(0xFF64FFDA),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
      )
      Text(
        text = "Crystalline impressions preserved in the subterranean Aether, capturing voices and events from the night of the sealing.",
        color = Color(0xFFA096A8),
        fontSize = 11.sp,
        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
      )
    }

    if (memories.isEmpty()) {
      item {
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF161220),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF382F48)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "No Aether memories have been resonated yet.\nInvestigate the abandoned research bay and containment relics in the Restricted Wing to discover memory echoes.",
              color = Color(0xFF888090),
              fontSize = 12.sp,
              lineHeight = 18.sp,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    } else {
      items(memories) { memory ->
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = Color(0xFF1A1429),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C4DFF).copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = memory.title,
                  color = Color(0xFFE1BEE7),
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = "Variant: ${memory.variantTag.replace('_', ' ').uppercase()}",
                  color = Color(0xFF80D8FF),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Medium
                )
              }

              if (onReplay != null) {
                Button(
                  onClick = { onReplay(memory.id) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF311B92),
                    contentColor = Color(0xFF64FFDA)
                  ),
                  shape = RoundedCornerShape(6.dp),
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.height(32.dp)
                ) {
                  Text(text = "REPLAY ECHO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "\"${memory.sensoryEcho}\"",
              color = Color(0xFFB39DDB),
              fontSize = 11.sp,
              fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
              lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Transcribed dialogue lines
            Column(
              verticalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier
                .background(Color(0xFF100C1D), RoundedCornerShape(6.dp))
                .padding(8.dp)
                .fillMaxWidth()
            ) {
              memory.dialogueLines.forEach { line ->
                Row {
                  Text(
                    text = "${line.speaker}: ",
                    color = Color(0xFF80D8FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "\"${line.line}\"",
                    color = Color(0xFFEDE7F6),
                    fontSize = 11.sp
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFF2A1538),
              border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFFFD54F))
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "ESTABLISHED TRUTH: ",
                  color = Color(0xFFFFD54F),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
                Text(
                  text = memory.revelationStatement,
                  color = Color(0xFFFFF8E1),
                  fontSize = 11.sp
                )
              }
            }
          }
        }
      }
    }
  }
}
