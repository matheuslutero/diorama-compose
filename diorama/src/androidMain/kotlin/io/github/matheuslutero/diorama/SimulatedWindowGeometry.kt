package io.github.matheuslutero.diorama

import kotlin.math.roundToInt

/**
 * Where the simulated screen currently is on the host display, in host pixels, and how much its
 * drawing is scaled by.
 *
 * A window opened by the app is not part of the composition, so the View code that has to place it
 * cannot read composition state. The composition writes here every time the stage is positioned and
 * the View code reads it back on the same thread, which is why plain vars are enough: this is a
 * measurement of what already happened, never something a layout depends on.
 */
internal class SimulatedWindowGeometry {
  /** The simulated screen in pixels — what the app is actually measured at. */
  var widthPx: Int = 0
    private set
  var heightPx: Int = 0
    private set

  /** The DeviceViewport fit scale, read back from the transform rather than recomputed. */
  var scale: Float = 1f
    private set

  var screenLeft: Int = 0
    private set
  var screenTop: Int = 0
    private set

  val isReady: Boolean get() = widthPx > 0 && heightPx > 0

  val scaledWidth: Int get() = (widthPx * scale).roundToInt()
  val scaledHeight: Int get() = (heightPx * scale).roundToInt()

  fun update(widthPx: Int, heightPx: Int, scale: Float, screenLeft: Int, screenTop: Int) {
    this.widthPx = widthPx
    this.heightPx = heightPx
    this.scale = scale
    this.screenLeft = screenLeft
    this.screenTop = screenTop
  }
}
