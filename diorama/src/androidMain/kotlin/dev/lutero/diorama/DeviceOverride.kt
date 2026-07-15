package dev.lutero.diorama

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import dev.lutero.diorama.frame.DeviceSpec
import dev.lutero.diorama.frame.Orientation
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Turns a [DeviceSpec] into an overridden Configuration plus a matching WindowInfo.
 *
 * Every axis is set explicitly. Configuration.updateFrom merges onto the host, so anything left
 * alone silently inherits the host's value — a host with 2x font scale leaks straight into the
 * simulation otherwise.
 */
@Composable
internal fun DeviceOverride(
  spec: DeviceSpec,
  orientation: Orientation,
  fontScale: Float,
  darkMode: Boolean,
  content: @Composable () -> Unit,
) {
  val base = LocalConfiguration.current
  val size = spec.sizeFor(orientation)

  val configuration = remember(base, spec, orientation, fontScale, darkMode) {
    Configuration().apply {
      updateFrom(base)
      densityDpi = spec.dpi
      screenWidthDp = size.width.value.roundToInt()
      screenHeightDp = size.height.value.roundToInt()
      smallestScreenWidthDp = min(screenWidthDp, screenHeightDp)
      this.orientation =
        if (size.width > size.height) {
          Configuration.ORIENTATION_LANDSCAPE
        } else {
          Configuration.ORIENTATION_PORTRAIT
        }
      this.fontScale = fontScale
      uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (darkMode) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
      screenLayout = (screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK.inv()) or
        screenLayoutSize(smallestScreenWidthDp)
    }
  }

  OverriddenConfiguration(configuration) {
    WindowInfoOverride(size, content)
  }
}

/**
 * currentWindowAdaptiveInfo() derives the window size from LocalWindowInfo.containerSize divided
 * by LocalDensity, so overriding both is what makes WindowSizeClass and adaptive layouts follow
 * the simulated device instead of the host.
 */
@Composable
private fun WindowInfoOverride(size: DpSize, content: @Composable () -> Unit) {
  val density = LocalDensity.current
  val current = LocalWindowInfo.current
  val overridden = remember(current, size, density) {
    object : WindowInfo by current {
      override val containerSize: IntSize
        get() = with(density) { IntSize(size.width.roundToPx(), size.height.roundToPx()) }
    }
  }
  CompositionLocalProvider(LocalWindowInfo provides overridden, content = content)
}

// TODO(screen-layout): mirror androidx's calculateScreenLayout, which also carries the long/round
//   bits. These are the documented smallestWidth buckets only.
private fun screenLayoutSize(smallestWidthDp: Int): Int = when {
  smallestWidthDp >= 720 -> Configuration.SCREENLAYOUT_SIZE_XLARGE
  smallestWidthDp >= 480 -> Configuration.SCREENLAYOUT_SIZE_LARGE
  smallestWidthDp >= 320 -> Configuration.SCREENLAYOUT_SIZE_NORMAL
  else -> Configuration.SCREENLAYOUT_SIZE_SMALL
}
