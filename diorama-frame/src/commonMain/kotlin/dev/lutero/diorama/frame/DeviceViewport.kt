package dev.lutero.diorama.frame

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Lays [content] out at exactly [deviceSize] and scales only its drawing to fit the space this
 * composable is given.
 *
 * The two halves must stay separate: the app is *constrained* to the true logical size, and is
 * only ever *scaled* visually. Scaling to constrain instead produces an app that reports one
 * size and lays out at another.
 *
 * `placeWithLayer` is load-bearing rather than stylistic — it creates a real OwnedLayer, so
 * NodeCoordinator hit-tests through the inverse matrix and pointer input maps correctly with no
 * manual transform. Drawing the same scale via Canvas.scale/drawWithContent has no layer, so
 * the pixels move while the touch targets stay behind.
 *
 * [content] must already be composing at the device's density; this reads LocalDensity to turn
 * [deviceSize] into pixels.
 */
@Composable
fun DeviceViewport(
  deviceSize: DpSize,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  Layout(content = content, modifier = modifier) { measurables, constraints ->
    val widthPx = deviceSize.width.roundToPx()
    val heightPx = deviceSize.height.roundToPx()
    val placeable = measurables.first().measure(Constraints.fixed(widthPx, heightPx))
    val scale = fitScale(constraints, widthPx, heightPx)

    layout((widthPx * scale).roundToInt(), (heightPx * scale).roundToInt()) {
      placeable.placeWithLayer(0, 0) {
        scaleX = scale
        scaleY = scale
        transformOrigin = TransformOrigin(0f, 0f)
        // Leave compositingStrategy and alpha alone. Anything that forces an offscreen buffer
        // (Offscreen, alpha < 1, a RenderEffect) rasterizes at natural size and then bilinear
        // filters it, which is what makes a scaled preview look soft.
      }
    }
  }
}

/** Constraints are always in pixels, so this compares like with like whatever the density is. */
private fun fitScale(constraints: Constraints, widthPx: Int, heightPx: Int): Float {
  if (widthPx <= 0 || heightPx <= 0) return 1f
  val widthRatio =
    if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() / widthPx else Float.MAX_VALUE
  val heightRatio =
    if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() / heightPx else Float.MAX_VALUE
  val scale = min(widthRatio, heightRatio)
  return if (scale == Float.MAX_VALUE) 1f else scale
}
