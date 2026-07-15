package dev.lutero.diorama.frame

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

  /** Bounds for the editor. Wide enough for a watch at one end and a large monitor at the other. */
  val WidthRange: ClosedRange<Dp> = 200.dp..2560.dp
  val HeightRange: ClosedRange<Dp> = 200.dp..2560.dp

  /**
   * Android's real density buckets, from `DisplayMetrics.DENSITY_*`. The editor steps through these
   * rather than offering a continuous range: no device reports 271dpi, and Android snaps arbitrary
   * values to a bucket anyway, so a free slider would only invite specifications that cannot exist.
   */
  val Densities: List<Int> =
    listOf(120, 160, 213, 240, 280, 320, 360, 400, 420, 440, 480, 520, 560, 640)

  /** Starts at the Medium Phone reference so the editor opens on something recognisable. */
  val Default: DeviceSpec = DeviceSpec(
    id = Id,
    name = "Custom",
    screenSize = DpSize(411.dp, 914.dp),
    dpi = 420,
    category = DeviceCategory.Custom,
    // The width and height sliders and the orientation toggle are two controls over one thing, and
    // sizeFor normalises a rotatable device to short-by-long for portrait. A catalog device wants
    // that, since a 1280x800 tablet is natively landscape. Here it would make the sliders lie: set
    // width to 2110 and the device renders 914 wide. Rotate a custom device by swapping the sliders.
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
