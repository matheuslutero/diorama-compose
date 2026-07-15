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

@Immutable
data class DeviceSpec(
  val id: String,
  val name: String,
  /** Logical size the app lays out in. One Compose dp here == one Flutter logical pixel. */
  val screenSize: DpSize,
  /**
   * The device's real dpi, always chosen and never derived from a fit ratio. Deriving it is
   * what makes androidx's ForcedSize unusable here: it reports whatever dpi makes the subtree
   * fit its container, so a 1280x800 tablet resolves at 159dpi (mdpi bucket) instead of 240
   * (hdpi), and the reported dpi shifts when the preview container is resized.
   */
  val dpi: Int,
  val safeAreas: DeviceInsets = DeviceInsets(),
  /** Null means the device cannot rotate. */
  val rotatedSafeAreas: DeviceInsets? = null,
  val platform: DevicePlatform = DevicePlatform.Android,
) {
  val canRotate: Boolean get() = rotatedSafeAreas != null

  val density: Float get() = dpi / 160f

  /**
   * Normalised rather than read straight off [screenSize], because a device's natural orientation
   * is not always portrait — a 1280x800 tablet would otherwise report landscape when asked for
   * portrait.
   *
   * A device that cannot rotate keeps [screenSize] whatever is asked of it: a desktop monitor is
   * 1920x1080 and has no portrait to normalise to.
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
    if (orientation == Orientation.Landscape && canRotate) rotatedSafeAreas!! else safeAreas
}
