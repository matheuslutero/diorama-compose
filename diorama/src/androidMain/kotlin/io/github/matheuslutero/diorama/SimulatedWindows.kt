package io.github.matheuslutero.diorama

import android.content.Context
import android.content.MutableContextWrapper
import android.widget.FrameLayout
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.roundToInt

/**
 * Hands the app a View of its own, so the windows it opens belong to the simulation.
 *
 * Popup and Dialog both capture `LocalView.current` and then reach for its Context. That Context is
 * where the WindowManager and the LayoutInflater come from, and — because the new window gets its
 * own composition — it is also what ProvideAndroidCompositionLocals re-derives Configuration,
 * density and layout direction from. Overriding LocalContext alone reaches none of it: the new
 * window asks the *view*, not the composition. Left alone the app's dialogs capture the host's
 * AndroidComposeView, which is why one used to open at the host's density, at the host's size, over
 * the whole host screen, dimming the panel with it.
 *
 * The View is a real ViewGroup and it is really attached: ripple hosts itself in LocalView, and a
 * window needs a live window token to be shown at all. It stays empty and zero-sized — its Context
 * is the whole point of it.
 *
 * There is exactly one View, for the life of the simulation, and switching device swaps its Context
 * rather than the View. AndroidView runs its factory once and keeps the View it made, so a View
 * built per device is never adopted and never attached — and every `findViewTreeOwner()` through an
 * unattached View answers null, which takes down anything resolving an owner that way.
 *
 * [content] is handed a Modifier to put on the simulated screen itself, which is what reports the
 * geometry every window is then placed against.
 */
@Composable
internal fun SimulatedWindows(
  geometry: SimulatedWindowGeometry,
  content: @Composable (screen: Modifier) -> Unit,
) {
  val context = LocalContext.current
  val hostView = LocalView.current
  val holderContext = remember { DioramaViewContext(context) }
  val holder = remember { FrameLayout(holderContext) }
  val location = remember { IntArray(2) }

  // Before the content composes, so a window opened this frame is built from the new device's
  // Context rather than the one it is replacing.
  holderContext.baseContext = context

  AndroidView(factory = { holder }, modifier = Modifier.size(0.dp))

  CompositionLocalProvider(LocalView provides holder) {
    content(
      Modifier.onGloballyPositioned { coordinates ->
        val size = coordinates.size
        if (size.width == 0 || size.height == 0) return@onGloballyPositioned
        // Read the scale back off the transform instead of recomputing the fit: this measures what
        // the viewport actually did, so the two cannot drift apart.
        val origin = coordinates.localToWindow(Offset.Zero)
        val edge = coordinates.localToWindow(Offset(size.width.toFloat(), 0f))
        val scale = (edge.x - origin.x) / size.width
        hostView.getLocationOnScreen(location)
        geometry.update(
          widthPx = size.width,
          heightPx = size.height,
          scale = if (scale > 0f) scale else 1f,
          screenLeft = (location[0] + origin.x).roundToInt(),
          screenTop = (location[1] + origin.y).roundToInt(),
        )
      },
    )
  }
}

/**
 * A MutableContextWrapper that exists to carry a name.
 *
 * `LocalView.current.context` is not the Activity inside the simulation, so `(context as Activity)`
 * throws — and a ClassCastException reports the class it was given. A bare MutableContextWrapper
 * names nobody; this one puts the simulation in the stack trace of the app that hit it.
 */
private class DioramaViewContext(
  base: Context,
) : MutableContextWrapper(base)
