package com.example.inventory

/**
 * Categorization of discoverable items.
 */
enum class ItemCategory {
  INGREDIENT,
  KEY_ITEM,
  DOCUMENT,
  CATALYST,
  TOOL
}

/**
 * Immutable definition of an item found in the academy.
 */
data class ItemDefinition(
  val id: String,
  val name: String,
  val description: String,
  val category: ItemCategory,
  val inspectableModelPath: String? = null
)

/**
 * Single slot in the player's inventory bag.
 */
data class InventorySlot(
  val item: ItemDefinition,
  val quantity: Int = 1
)

/**
 * Interface contract for inventory operations.
 */
interface InventoryHolder {
  fun addItem(item: ItemDefinition, quantity: Int = 1): Boolean
  fun removeItem(itemId: String, quantity: Int = 1): Boolean
  fun hasItem(itemId: String): Boolean
  fun getSlots(): List<InventorySlot>
}
