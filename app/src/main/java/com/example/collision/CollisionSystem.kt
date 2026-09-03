package com.example.collision

/**
 * Axis-Aligned Bounding Box (2D top-down X-Z projection).
 */
data class AABB(
  val minX: Float,
  val minZ: Float,
  val maxX: Float,
  val maxZ: Float
) {
  fun intersectsCircle(cx: Float, cz: Float, radius: Float): Boolean {
    val closestX = cx.coerceIn(minX, maxX)
    val closestZ = cz.coerceIn(minZ, maxZ)
    val dx = cx - closestX
    val dz = cz - closestZ
    return (dx * dx + dz * dz) < (radius * radius)
  }
}

/**
 * Circle obstacle for cylindrical props (cauldrons, pedestals).
 */
data class CircleObstacle(
  val cx: Float,
  val cz: Float,
  val radius: Float
) {
  fun intersects(px: Float, pz: Float, playerRadius: Float): Boolean {
    val dx = px - cx
    val dz = pz - cz
    val minDist = radius + playerRadius
    return (dx * dx + dz * dz) < (minDist * minDist)
  }
}

/**
 * Manages player environment collision and wall-sliding for the academy chambers.
 */
class CollisionSystem(
  val roomMinX: Float = -4.65f,
  val roomMaxX: Float = 4.65f,
  val roomMinZ: Float = -5.65f,
  val roomMaxZ: Float = 5.65f,
  val playerRadius: Float = 0.35f
) {
  val boxes = mutableListOf<AABB>()
  val circles = mutableListOf<CircleObstacle>()

  // North Doorway barrier (academy gate threshold at Z = -5.65)
  private val doorBox = AABB(-1.2f, -6.2f, 1.2f, -5.65f)

  init {
    // 4 Vaulted Stone Pillars (width 0.6 x 0.6)
    boxes.add(AABB(-2.55f, -2.35f, -1.85f, -1.65f)) // NW Pillar
    boxes.add(AABB(1.85f, -2.35f, 2.55f, -1.65f))  // NE Pillar
    boxes.add(AABB(-2.55f, 1.65f, -1.85f, 2.35f))  // SW Pillar
    boxes.add(AABB(1.85f, 1.65f, 2.55f, 2.35f))   // SE Pillar

    // Alchemy Table (Width 2.0, Depth 1.0 at Z = -3.5)
    boxes.add(AABB(-1.15f, -4.15f, 1.15f, -2.85f))

    // North Doorway barrier initially closed
    boxes.add(doorBox)

    // Ancient Cauldron prop at (-2.2, -0.5)
    circles.add(CircleObstacle(-2.2f, -0.5f, 0.55f))

    // Mystic Crystal Pedestal at (2.2, -0.5)
    circles.add(CircleObstacle(2.2f, -0.5f, 0.45f))
  }

  var isDoorOpen: Boolean = false
    private set

  /**
   * Dynamically toggles doorway collision when the door opens or closes.
   */
  fun setDoorBarrier(isOpen: Boolean) {
    isDoorOpen = isOpen
    if (isOpen) {
      boxes.remove(doorBox)
    } else if (!boxes.contains(doorBox)) {
      boxes.add(doorBox)
    }
  }

  /**
   * Checks whether a position (x, z) with playerRadius collides with room boundaries or obstacles.
   */
  fun isColliding(x: Float, z: Float): Boolean {
    // Outer Room Boundary Check (allow doorway threshold through north opening when door is open)
    val inDoorway = isDoorOpen && x in -1.1f..1.1f && z in -6.5f..-5.65f
    if (!inDoorway) {
      if (x - playerRadius < roomMinX || x + playerRadius > roomMaxX) return true
      if (z - playerRadius < roomMinZ || z + playerRadius > roomMaxZ) return true
    }

    // Box Obstacles
    for (i in boxes.indices) {
      if (boxes[i].intersectsCircle(x, z, playerRadius)) return true
    }

    // Circular Obstacles
    for (i in circles.indices) {
      if (circles[i].intersects(x, z, playerRadius)) return true
    }

    return false
  }

  /**
   * Resolves player movement with independent axis testing for smooth wall-sliding.
   * If movement along one axis is blocked, player continues sliding along the unblocked axis.
   */
  fun resolveMovement(
    currentX: Float,
    currentZ: Float,
    deltaX: Float,
    deltaZ: Float
  ): Pair<Float, Float> {
    var newX = currentX
    var newZ = currentZ

    // 1. Try moving along X axis
    val candidateX = (currentX + deltaX).coerceIn(roomMinX + playerRadius, roomMaxX - playerRadius)
    if (!isColliding(candidateX, currentZ)) {
      newX = candidateX
    }

    // 2. Try moving along Z axis
    val candidateZ = (currentZ + deltaZ).coerceIn(roomMinZ + playerRadius, roomMaxZ - playerRadius)
    if (!isColliding(newX, candidateZ)) {
      newZ = candidateZ
    }

    return Pair(newX, newZ)
  }
}
