package dev.lutero.diorama.frame

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Immutable
data class DeviceInsets(
  val left: Dp = 0.dp,
  val top: Dp = 0.dp,
  val right: Dp = 0.dp,
  val bottom: Dp = 0.dp,
)

enum class DevicePlatform { Android, Ios, Desktop }

enum class Orientation { Portrait, Landscape }

/** Groups the catalog in the picker. Carries no behaviour. */
enum class DeviceCategory { Phone, Foldable, Tablet, Desktop, Custom }

@Immutable
data class DeviceSpec(
  val id: String,
  val name: String,
  /**
   * Logical size the app lays out in. One Compose dp here is one Flutter logical pixel.
   *
   * Whole dp on purpose: Android computes `screenWidthDp` as `(int)(widthPx / density)`, so a
   * Pixel 6 at 1080px and 2.625 density genuinely reports 411, not 411.43. Rounding here matches
   * what the hardware reports rather than approximating it.
   */
  val screenSize: DpSize,
  /**
   * The device's real dpi, always chosen and never derived from a fit ratio. Deriving it is what
   * makes androidx's ForcedSize unusable here: it reports whatever dpi makes the subtree fit its
   * container, so a 1280x800 tablet resolves at 159dpi (mdpi bucket) instead of 240 (hdpi), and the
   * reported dpi shifts when the preview container is resized.
   */
  val dpi: Int,
  val category: DeviceCategory = DeviceCategory.Phone,
  /** Explicit rather than inferred from [rotatedSafeAreas]: a monitor has safe areas but no rotation. */
  val canRotate: Boolean = true,
  val safeAreas: DeviceInsets = DeviceInsets(),
  val rotatedSafeAreas: DeviceInsets? = null,
  val platform: DevicePlatform = DevicePlatform.Android,
) {
  val density: Float get() = dpi / 160f

  /**
   * Normalised rather than read straight off [screenSize], because a device's natural orientation is
   * not always portrait: a 1280x800 tablet would otherwise report landscape when asked for portrait.
   *
   * A device that cannot rotate keeps [screenSize] whatever is asked of it.
   */
  fun sizeFor(orientation: Orientation): DpSize {
    if (!canRotate) return screenSize
    val short = minOf(screenSize.width, screenSize.height)
    val long = maxOf(screenSize.width, screenSize.height)
    return when (orientation) {
      Orientation.Portrait -> DpSize(short, long)
      Orientation.Landscape -> DpSize(long, short)
    }
  }

  fun safeAreasFor(orientation: Orientation): DeviceInsets =
    if (orientation == Orientation.Landscape) rotatedSafeAreas ?: safeAreas else safeAreas
}
