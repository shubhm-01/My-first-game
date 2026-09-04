package com.example.puzzle

import com.example.core.GameEvent
import com.example.core.GameEventBus

/**
 * Ancient alchemical principles represented in the Academy's sealing wards.
 */
enum class AlchemicalSymbol(
  val symbolId: String,
  val displayName: String,
  val glyph: String,
  val principle: String,
  val elementColorHex: Long
) {
  SULFUR(
    symbolId = "symbol_sulfur",
    displayName = "Sulfur",
    glyph = "🜍",
    principle = "Soul / Aether Flame",
    elementColorHex = 0xFFFFB74D // Golden orange
  ),
  MERCURY(
    symbolId = "symbol_mercury",
    displayName = "Mercury",
    glyph = "☿",
    principle = "Mind / Volatile Spirit",
    elementColorHex = 0xFF81D4FA // Luminous cyan
  ),
  SALT(
    symbolId = "symbol_salt",
    displayName = "Salt",
    glyph = "🜔",
    principle = "Body / Earth Ground",
    elementColorHex = 0xFFAED581 // Verdant earth
  ),
  SILVER(
    symbolId = "symbol_silver",
    displayName = "Silver",
    glyph = "☽",
    principle = "Essence / Lunar Quencher",
    elementColorHex = 0xFFE0E0E0 // Lunar silver
  );

  companion object {
    fun fromId(id: String?): AlchemicalSymbol? {
      return entries.find { it.symbolId.equals(id, ignoreCase = true) || it.name.equals(id, ignoreCase = true) }
    }
  }
}

/**
 * Data specification for an alchemical puzzle variant.
 */
data class PuzzleConfiguration(
  val configId: String,
  val title: String,
  val targetSequence: List<AlchemicalSymbol>,
  val clueEvidenceId: String,
  val riddleHint: String,
  val slotLabels: List<String> = listOf("Outer Ward", "Resonance Gate", "Keystone Sigil")
)

/**
 * Environmental alchemical lock securing the Academy Restricted Wing.
 * Requires configuring a 3-element harmonic sequence derived from investigation clues.
 */
class AlchemicalSealingPuzzle(
  override val puzzleId: String = "puzzle_restricted_wing_seal",
  initialConfig: PuzzleConfiguration = DEFAULT_CONFIG_FLAME
) : PuzzleNode {

  override val entityId: String get() = puzzleId

  override var status: PuzzleStatus = PuzzleStatus.ACTIVE
    private set

  var activeConfig: PuzzleConfiguration = initialConfig
    private set

  // 3-slot tumbler representation
  val currentSlots: MutableList<AlchemicalSymbol?> = mutableListOf(null, null, null)

  var lastEvaluationSuccess: Boolean? = null
    private set

  fun configure(newConfig: PuzzleConfiguration, eventBus: GameEventBus? = null) {
    activeConfig = newConfig
    reset()
    eventBus?.emit(GameEvent.PuzzleConfigured(puzzleId = puzzleId, configurationId = newConfig.configId))
  }

  fun cycleSlot(slotIndex: Int, forward: Boolean = true): AlchemicalSymbol {
    if (slotIndex !in 0 until 3) return AlchemicalSymbol.SULFUR
    val symbols = AlchemicalSymbol.entries
    val current = currentSlots[slotIndex]
    val next = if (current == null) {
      if (forward) symbols.first() else symbols.last()
    } else {
      val currentIndex = symbols.indexOf(current)
      val nextIndex = if (forward) {
        (currentIndex + 1) % symbols.size
      } else {
        (currentIndex - 1 + symbols.size) % symbols.size
      }
      symbols[nextIndex]
    }
    currentSlots[slotIndex] = next
    lastEvaluationSuccess = null
    return next
  }

  fun setSlot(slotIndex: Int, symbol: AlchemicalSymbol?) {
    if (slotIndex in 0 until 3) {
      currentSlots[slotIndex] = symbol
      lastEvaluationSuccess = null
    }
  }

  override fun evaluate(): Boolean {
    return evaluateWithBus(null)
  }

  fun evaluateWithBus(eventBus: GameEventBus?): Boolean {
    if (status == PuzzleStatus.SOLVED) return true

    val isCorrect = currentSlots.size == activeConfig.targetSequence.size &&
      currentSlots.indices.all { i -> currentSlots[i] == activeConfig.targetSequence[i] }

    lastEvaluationSuccess = isCorrect

    if (isCorrect) {
      status = PuzzleStatus.SOLVED
      eventBus?.emit(GameEvent.PuzzleSolved(puzzleId = puzzleId))
      eventBus?.emit(GameEvent.PuzzleStateChanged(puzzleId = puzzleId, isSolved = true))
    } else {
      status = PuzzleStatus.ACTIVE
    }

    return isCorrect
  }

  override fun reset() {
    resetWithBus(null)
  }

  fun resetWithBus(eventBus: GameEventBus?) {
    for (i in 0 until 3) {
      currentSlots[i] = null
    }
    lastEvaluationSuccess = null
    if (status != PuzzleStatus.SOLVED) {
      status = PuzzleStatus.ACTIVE
    }
    eventBus?.emit(GameEvent.PuzzleReset(puzzleId = puzzleId))
  }

  override fun captureState(): Map<String, String> {
    return mapOf(
      "puzzleId" to puzzleId,
      "status" to status.name,
      "configId" to activeConfig.configId,
      "slot0" to (currentSlots.getOrNull(0)?.symbolId ?: "none"),
      "slot1" to (currentSlots.getOrNull(1)?.symbolId ?: "none"),
      "slot2" to (currentSlots.getOrNull(2)?.symbolId ?: "none")
    )
  }

  override fun restoreState(state: Map<String, String>) {
    state["status"]?.let {
      try {
        status = PuzzleStatus.valueOf(it)
      } catch (ignored: IllegalArgumentException) {}
    }
    for (i in 0 until 3) {
      val symId = state["slot$i"]
      currentSlots[i] = if (symId != null && symId != "none") AlchemicalSymbol.fromId(symId) else null
    }
  }

  companion object {
    /**
     * Variant A: Flame Binding Protocol
     * Target: SULFUR -> MERCURY -> SALT
     * Corroborates with Elias Research Note / Ward Protocol Note.
     */
    val DEFAULT_CONFIG_FLAME = PuzzleConfiguration(
      configId = "config_flame_binding",
      title = "Flame Binding Protocol",
      targetSequence = listOf(
        AlchemicalSymbol.SULFUR,
        AlchemicalSymbol.MERCURY,
        AlchemicalSymbol.SALT
      ),
      clueEvidenceId = "evidence_ward_protocol_note",
      riddleHint = "Quell soul flame with Sulfur, bind volatile resonance in Mercury, anchor the seal with Salt."
    )

    /**
     * Variant B: Lunar Quenching Protocol
     * Target: SILVER -> SALT -> SULFUR
     * Corroborates with Archival Sealing Diagram.
     */
    val DEFAULT_CONFIG_LUNAR = PuzzleConfiguration(
      configId = "config_lunar_quenching",
      title = "Lunar Quenching Protocol",
      targetSequence = listOf(
        AlchemicalSymbol.SILVER,
        AlchemicalSymbol.SALT,
        AlchemicalSymbol.SULFUR
      ),
      clueEvidenceId = "evidence_sealing_diagram",
      riddleHint = "Cool the threshold with Silver, solidify the barrier in Salt, seal the gateway with Sulfur."
    )

    /**
     * All certified puzzle variants for controlled replayability.
     */
    val CERTIFIED_CONFIGURATIONS = listOf(
      DEFAULT_CONFIG_FLAME,
      DEFAULT_CONFIG_LUNAR
    )
  }
}
