package com.example.renderer

import android.content.Context
import io.github.sceneview.SceneView

/**
 * Validates that SceneView can be instantiated and configured within the Android context.
 */
class SceneViewProbe {
  fun canInstantiate(context: Context): Boolean {
    return try {
      val view = SceneView(context)
      view != null
    } catch (e: Throwable) {
      false
    }
  }
}
