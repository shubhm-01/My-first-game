package com.example.player

import com.example.collision.CollisionSystem
import kotlin.math.cos
import kotlin.math.sin

/**
 * First-person player controller managing position, camera look angles,
 * collision sliding, and input translation.
 */
class PlayerController(
  val collisionSystem: CollisionSystem = CollisionSystem(),
  initialPosition: Position3D = Position3D(0.0f, 1.7f, 2.8f),
  initialRotation: Rotation2D = Rotation2D(yaw = 0.0f, pitch = 0.0f)
) {
  var position: Position3D = initialPosition
    private set

  var rotation: Rotation2D = initialRotation
    private set

  var isMoving: Boolean = false
    private set

  // Configuration
  var movementSpeed: Float = 2.8f
  var lookSensitivity: Float = 0.16f
  val minPitch: Float = -85.0f
  val maxPitch: Float = 85.0f
  val eyeHeight: Float = 1.7f

  // Raw joystick input values in range [-1.0, 1.0]
  private var joystickX: Float = 0.0f
  private var joystickY: Float = 0.0f

  /**
   * Updates virtual joystick axes.
   * [x] is lateral strafe (-1.0 = left, +1.0 = right)
   * [y] is forward/backward (+1.0 = forward, -1.0 = backward)
   */
  fun setMovementInput(x: Float, y: Float) {
    joystickX = x.coerceIn(-1.0f, 1.0f)
    joystickY = y.coerceIn(-1.0f, 1.0f)
  }

  /**
   * Applies touch look delta from the right-hand swipe surface.
   * [deltaX] rotates yaw (horizontal camera pan).
   * [deltaY] rotates pitch (vertical camera tilt).
   */
  fun applyLookDelta(deltaX: Float, deltaY: Float) {
    val newYaw = (rotation.yaw - deltaX * lookSensitivity) % 360.0f
    val normalizedYaw = if (newYaw < 0.0f) newYaw + 360.0f else newYaw
    val newPitch = (rotation.pitch - deltaY * lookSensitivity).coerceIn(minPitch, maxPitch)

    rotation = Rotation2D(yaw = normalizedYaw, pitch = newPitch)
  }

  /**
   * Advances player movement and resolves environment collision for frame [deltaSeconds].
   */
  fun update(deltaSeconds: Float): PlayerState {
    val inputMag = kotlin.math.sqrt(joystickX * joystickX + joystickY * joystickY)
    if (inputMag > 0.05f) {
      isMoving = true
      val normX = joystickX / (if (inputMag > 1.0f) inputMag else 1.0f)
      val normY = joystickY / (if (inputMag > 1.0f) inputMag else 1.0f)

      val yawRad = Math.toRadians(rotation.yaw.toDouble()).toFloat()
      // Forward vector (-Z is forward when yaw is 0)
      val forwardX = -sin(yawRad)
      val forwardZ = -cos(yawRad)
      // Strafe vector (+X is right when yaw is 0)
      val rightX = cos(yawRad)
      val rightZ = -sin(yawRad)

      val moveStepX = (forwardX * normY + rightX * normX) * movementSpeed * deltaSeconds
      val moveStepZ = (forwardZ * normY + rightZ * normX) * movementSpeed * deltaSeconds

      val (resolvedX, resolvedZ) = collisionSystem.resolveMovement(
        currentX = position.x,
        currentZ = position.z,
        deltaX = moveStepX,
        deltaZ = moveStepZ
      )

      position = Position3D(x = resolvedX, y = eyeHeight, z = resolvedZ)
    } else {
      isMoving = false
    }

    return PlayerState(
      position = position,
      rotation = rotation,
      isMoving = isMoving,
      movementSpeed = movementSpeed
    )
  }

  /**
   * Computes normalized 3D forward ray direction vector based on current camera yaw and pitch.
   */
  fun getForwardRayDirection(): Triple<Float, Float, Float> {
    val yawRad = Math.toRadians(rotation.yaw.toDouble()).toFloat()
    val pitchRad = Math.toRadians(rotation.pitch.toDouble()).toFloat()

    val cosPitch = cos(pitchRad)
    val dirX = -sin(yawRad) * cosPitch
    val dirY = sin(pitchRad)
    val dirZ = -cos(yawRad) * cosPitch

    return Triple(dirX, dirY, dirZ)
  }

  fun reset(spawnPos: Position3D = Position3D(0.0f, 1.7f, 2.8f), spawnYaw: Float = 0.0f) {
    position = spawnPos
    rotation = Rotation2D(yaw = spawnYaw, pitch = 0.0f)
    joystickX = 0.0f
    joystickY = 0.0f
    isMoving = false
  }
}
