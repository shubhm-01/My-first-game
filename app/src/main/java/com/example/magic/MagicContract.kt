package com.example.magic

/**
 * The five core magical abilities of the Alchemist.
 */
enum class AbilityType {
  FIRE,     // Ignite furnaces, melt seals, trigger heat plates
  FROST,    // Freeze water, lock rotating mechanisms, create platforms
  LIGHT,    // Reveal invisible ink, runes, illuminate chambers
  TIME,     // Revert broken mechanisms, view past echoes
  GRAVITY   // Manipulate heavy weights, alter physical orientation
}

/**
 * Functional definition of a player ability.
 */
data class AbilityDefinition(
  val type: AbilityType,
  val displayName: String,
  val description: String,
  val cooldownSeconds: Float = 1.0f
)

/**
 * Contract for in-world objects sensitive to magical spells.
 */
interface SpellTarget {
  val targetId: String
  val requiredAbility: AbilityType
  fun onSpellApplied(ability: AbilityType): Boolean
}
