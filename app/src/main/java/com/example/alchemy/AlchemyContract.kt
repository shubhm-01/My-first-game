package com.example.alchemy

import com.example.data.SaveableEntity

/**
 * Fundamental elemental affinities used in alchemy recipes.
 */
enum class ElementAspect {
  FIRE,
  WATER,
  EARTH,
  AIR,
  AETHER
}

/**
 * Botanical, mineral, or mystical component for brewing.
 */
data class AlchemyIngredient(
  val id: String,
  val name: String,
  val primaryAspect: ElementAspect,
  val potency: Int = 1
)

/**
 * Valid discovered recipe resulting in a potion.
 */
data class PotionRecipe(
  val id: String,
  val resultName: String,
  val requiredIngredientIds: List<String>,
  val heatRequired: Boolean = false,
  val effectDescription: String
)

/**
 * Contract for alchemy workstations (cauldron, distillation retort).
 */
interface AlchemyStation : SaveableEntity {
  val stationId: String
  val isHeated: Boolean
  fun addIngredient(ingredient: AlchemyIngredient)
  fun clearVessel()
  fun brew(): PotionRecipe?
}
