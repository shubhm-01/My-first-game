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
 * Manages player environment collision and wall-sliding for the academy chambers,
 * including seamless bidirectional traversal through the sanctum doorway to the exterior landing.
 */
class CollisionSystem(
  val roomMinX: Float = -4.65f,
  val roomMaxX: Float = 4.65f,
  val roomMinZ: Float = -5.65f,
  val roomMaxZ: Float = 5.65f,
  val landingMinX: Float = -2.15f,
  val landingMaxX: Float = 2.15f,
  val landingMinZ: Float = -9.20f,
  val playerRadius: Float = 0.35f
) {
  val boxes = mutableListOf<AABB>()
  val circles = mutableListOf<CircleObstacle>()

  // North Doorway barrier (blocking threshold at Z = -5.9 when door is closed)
  private val doorBox = AABB(-1.0f, -6.15f, 1.0f, -5.65f)

  init {
    // 1. Vaulted Stone Pillars in chamber (width 0.7 x 0.7)
    boxes.add(AABB(-2.55f, -2.35f, -1.85f, -1.65f)) // NW Pillar
    boxes.add(AABB(1.85f, -2.35f, 2.55f, -1.65f))  // NE Pillar
    boxes.add(AABB(-2.55f, 1.65f, -1.85f, 2.35f))  // SW Pillar
    boxes.add(AABB(1.85f, 1.65f, 2.55f, 2.35f))   // SE Pillar

    // 2. Alchemy Workbench (Width 2.0, Depth 1.0 at Z = -3.5)
    boxes.add(AABB(-1.15f, -4.15f, 1.15f, -2.85f))

    // 3. Ancient Cauldron prop at (-2.2, -0.5)
    circles.add(CircleObstacle(-2.2f, -0.5f, 0.55f))

    // 4. Mystic Crystal Pedestal at (2.2, -0.5)
    circles.add(CircleObstacle(2.2f, -0.5f, 0.45f))

    // 5. North Chamber Wall with Doorway Opening:
    // Left segment (West of doorway, opening starts at X = -0.95)
    boxes.add(AABB(-5.5f, -6.25f, -0.95f, -5.65f))
    // Right segment (East of doorway, opening ends at X = 0.95)
    boxes.add(AABB(0.95f, -6.25f, 5.5f, -5.65f))

    // 6. Minimal Safe Exterior Landing Terrace Balustrades (beyond doorway at Z < -5.8)
    // Left exterior terrace balustrade
    boxes.add(AABB(-5.0f, -10.0f, landingMinX, -5.8f))
    // Right exterior terrace balustrade
    boxes.add(AABB(landingMaxX, -10.0f, 5.0f, -5.8f))
    // Far exterior landing balustrade (North end of terrace)
    boxes.add(AABB(-5.0f, -12.0f, 5.0f, landingMinZ))

    // North Doorway barrier initially closed
    boxes.add(doorBox)
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
   * Checks whether a position (x, z) with playerRadius collides with environment geometry or obstacles.
   */
  fun isColliding(x: Float, z: Float): Boolean {
    // 1. Check all box obstacles (pillars, table, doorway barrier if closed, balustrades)
    for (i in boxes.indices) {
      if (boxes[i].intersectsCircle(x, z, playerRadius)) return true
    }

    // 2. Check all circular props (cauldron, crystal)
    for (i in circles.indices) {
      if (circles[i].intersects(x, z, playerRadius)) return true
    }

    // 3. Environment boundary check
    val inDoorwayOrLanding = isDoorOpen && x in -1.05f..1.05f && z in (landingMinZ - 0.5f)..roomMaxZ
    val onLanding = z < roomMinZ && x in landingMinX..landingMaxX && z >= landingMinZ

    if (!inDoorwayOrLanding && !onLanding) {
      // Inside chamber boundaries
      if (x - playerRadius < roomMinX || x + playerRadius > roomMaxX) return true
      if (z - playerRadius < roomMinZ || z + playerRadius > roomMaxZ) return true
    }

    return false
  }

  /**
   * Resolves player movement with independent axis testing for buttery-smooth wall-sliding.
   * Enables smooth bidirectional traversal through open portals and doorways.
   */
  fun resolveMovement(
    currentX: Float,
    currentZ: Float,
    deltaX: Float,
    deltaZ: Float
  ): Pair<Float, Float> {
    var newX = currentX
    var newZ = currentZ

    val isOutside = currentZ < roomMinZ
    val canTraverseDoorway = isDoorOpen && currentX in -1.05f..1.05f

    // 1. Determine X limits depending on whether in chamber or landing
    val minLimitX = if (isOutside) landingMinX + playerRadius else roomMinX + playerRadius
    val maxLimitX = if (isOutside) landingMaxX - playerRadius else roomMaxX - playerRadius

    val candidateX = (currentX + deltaX).coerceIn(minLimitX, maxLimitX)
    if (!isColliding(candidateX, currentZ)) {
      newX = candidateX
    }

    // 2. Determine Z limits (unlocked through doorway and on landing)
    val minLimitZ = if (canTraverseDoorway || isOutside) landingMinZ + playerRadius else roomMinZ + playerRadius
    val maxLimitZ = roomMaxZ - playerRadius

    val candidateZ = (currentZ + deltaZ).coerceIn(minLimitZ, maxLimitZ)
    if (!isColliding(newX, candidateZ)) {
      newZ = candidateZ
    }

    return Pair(newX, newZ)
  }
}
