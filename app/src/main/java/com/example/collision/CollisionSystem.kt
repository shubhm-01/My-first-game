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
  val restrictedMinZ: Float = -14.0f,
  val playerRadius: Float = 0.35f
) {
  val boxes = mutableListOf<AABB>()
  val circles = mutableListOf<CircleObstacle>()

  // North Doorway barrier (blocking threshold at Z = -5.9 when door is closed)
  private val doorBox = AABB(-1.0f, -6.15f, 1.0f, -5.65f)

  init {
    // 1. Vaulted Stone Pillars in Atrium (width 0.7 x 0.7)
    boxes.add(AABB(-2.55f, -2.35f, -1.85f, -1.65f)) // NW Pillar
    boxes.add(AABB(1.85f, -2.35f, 2.55f, -1.65f))  // NE Pillar
    boxes.add(AABB(-2.55f, 1.65f, -1.85f, 2.35f))  // SW Pillar
    boxes.add(AABB(1.85f, 1.65f, 2.55f, 2.35f))   // SE Pillar

    // 2. Alchemy Workbench in Atrium (Width 2.0, Depth 1.0 at Z = -3.5)
    boxes.add(AABB(-1.15f, -4.15f, 1.15f, -2.85f))

    // 3. Ancient Cauldron prop at (-2.2, -0.5)
    circles.add(CircleObstacle(-2.2f, -0.5f, 0.55f))

    // 4. Mystic Crystal Pedestal at (2.2, -0.5)
    circles.add(CircleObstacle(2.2f, -0.5f, 0.45f))

    // 5. North Chamber Wall with Doorway Opening (Z = -5.65)
    // Left segment (West of doorway, opening starts at X = -0.95)
    boxes.add(AABB(-5.5f, -6.25f, -0.95f, -5.65f))
    // Right segment (East of doorway, opening ends at X = 0.95)
    boxes.add(AABB(0.95f, -6.25f, 5.5f, -5.65f))

    // 6. Restricted Wing Corridor Walls with Room Doorways (Doorways at Z in -9.5f..-7.5f)
    // West corridor wall: south segment (-5.65 to -7.5) and north segment (-9.5 to -14.0)
    boxes.add(AABB(-2.5f, -7.5f, -1.45f, -5.65f))
    boxes.add(AABB(-2.5f, -14.0f, -1.45f, -9.5f))

    // East corridor wall: south segment (-5.65 to -7.5) and north segment (-9.5 to -14.0)
    boxes.add(AABB(1.45f, -7.5f, 2.5f, -5.65f))
    boxes.add(AABB(1.45f, -14.0f, 2.5f, -9.5f))

    // 7. Restricted Wing Room Outer Walls
    // West Research Bay Outer Walls
    boxes.add(AABB(-6.5f, -12.0f, -5.5f, -6.0f))   // West wall
    boxes.add(AABB(-6.0f, -12.0f, -1.5f, -11.0f))  // North wall
    boxes.add(AABB(-6.0f, -6.5f, -1.5f, -5.5f))    // South wall

    // East Archive Outer Walls
    boxes.add(AABB(5.5f, -12.0f, 6.5f, -6.0f))    // East wall
    boxes.add(AABB(1.5f, -12.0f, 6.0f, -11.0f))   // North wall
    boxes.add(AABB(1.5f, -6.5f, 6.0f, -5.5f))     // South wall

    // 8. North Deep Gate Obstacle (Bars entrance to deeper subterranean sector)
    boxes.add(AABB(-2.5f, -15.0f, 2.5f, -14.0f))

    // 9. Restricted Wing Interior Props & Obstacles
    // West Bay: Overturned research desk
    boxes.add(AABB(-3.9f, -9.0f, -3.1f, -8.0f))
    // West Bay: Shattered containment apparatus
    circles.add(CircleObstacle(-4.2f, -10.0f, 0.45f))

    // East Archive: Classified archive shelf
    boxes.add(AABB(3.2f, -8.0f, 4.2f, -7.0f))
    // East Archive: Resonating Aether Core plinth
    circles.add(CircleObstacle(3.5f, -9.8f, 0.45f))

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
    // 1. Check all box obstacles
    for (i in boxes.indices) {
      if (boxes[i].intersectsCircle(x, z, playerRadius)) return true
    }

    // 2. Check all circular props
    for (i in circles.indices) {
      if (circles[i].intersects(x, z, playerRadius)) return true
    }

    // 3. Environment boundary check
    val insideAtrium = x in roomMinX..roomMaxX && z in roomMinZ..roomMaxZ

    val inDoorway = isDoorOpen && x in -1.2f..1.2f && z in (restrictedMinZ - 0.5f)..roomMaxZ

    val inRestrictedWing = isDoorOpen && z < roomMinZ && (
      (x in -2.5f..2.5f && z in (restrictedMinZ - 0.5f)..roomMinZ) || // Corridor & Landing
      (x in -6.0f..-1.45f && z in -12.0f..-5.65f) ||                  // West Bay
      (x in 1.45f..6.0f && z in -12.0f..-5.65f)                       // East Archive
    )

    if (!insideAtrium && !inDoorway && !inRestrictedWing) {
      return true
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

    // 1. Test X-axis movement with wall sliding
    val candidateX = currentX + deltaX
    if (!isColliding(candidateX, currentZ)) {
      newX = candidateX
    }

    // 2. Test Z-axis movement with wall sliding
    val candidateZ = currentZ + deltaZ
    if (!isColliding(newX, candidateZ)) {
      newZ = candidateZ
    }

    return Pair(newX, newZ)
  }
}
