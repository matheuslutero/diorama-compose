package io.github.matheuslutero.diorama.frame

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The one device whose specification is edited at runtime rather than looked up.
 *
 * It is an ordinary [DeviceSpec], not a special case in the override path: everything downstream
 * treats it like any catalog entry. Only its identity is reserved, so the picker and the state saver
 * can recognise it.
 */
object CustomDevice {
  const val Id: String = "custom"

  val WidthRange: ClosedRange<Dp> = 200.dp..2560.dp
  val HeightRange: ClosedRange<Dp> = 200.dp..2560.dp

  /** Android's real density buckets, from `DisplayMetrics.DENSITY_*`; arbitrary dpi values do not
   * exist on hardware. */
  val Densities: List<Int> =
    listOf(120, 160, 213, 240, 280, 320, 360, 400, 420, 440, 480, 520, 560, 640)

  val Default: DeviceSpec = DeviceSpec(
    id = Id,
    name = "Custom",
    screenSize = DpSize(411.dp, 914.dp),
    dpi = 420,
    category = DeviceCategory.Custom,
    // sizeFor normalises rotatable devices to short-by-long, which would make the width/height
    // sliders lie; a custom device rotates by swapping the sliders instead.
    canRotate = false,
  )

  fun of(width: Dp, height: Dp, dpi: Int): DeviceSpec = Default.copy(
    screenSize = DpSize(
      width.coerceIn(WidthRange.start, WidthRange.endInclusive),
      height.coerceIn(HeightRange.start, HeightRange.endInclusive),
    ),
    dpi = nearestDensity(dpi),
  )

  fun nearestDensity(dpi: Int): Int = Densities.minBy { kotlin.math.abs(it - dpi) }
}
