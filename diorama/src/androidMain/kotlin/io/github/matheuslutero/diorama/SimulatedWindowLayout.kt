package io.github.matheuslutero.diorama

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

/**
 * Hosts the content of one Android window as if that window were the simulated screen.
 *
 * The child is measured at the device's pixel size and only its drawing is scaled, the split
 * [io.github.matheuslutero.diorama.frame.DeviceViewport] makes in the composition. It has to be a
 * real ViewGroup: nothing inverts a transform on a window's *root* view, since ViewRootImpl hands
 * the root raw window coordinates, so the pixels would move and the touch targets would stay
 * behind. ViewGroup.dispatchTouchEvent inverts a child's matrix.
 *
 * With a [window] this is a dialog. The window is filled and the device's rectangle placed inside
 * it, rather than the window being moved onto the device: bounds that change after a window is
 * shown resize its surface, and the compositor stretches the last buffer across the new bounds
 * until the next one arrives. Compose also calls setLayout from a SideEffect of its own once the
 * window is up, so no placement would stay put.
 *
 * Without a [window] this is a popup, and it wraps its content so the window stays where Compose
 * anchored it.
 */
@SuppressLint("ViewConstructor")
internal class SimulatedWindowLayout(
  context: Context,
  private val geometry: SimulatedWindowGeometry,
  private val window: Window? = null,
  private val contentGravity: Int = Gravity.CENTER,
  private val dimAmount: Float = 0f,
  /**
   * What the window last asked for, which decides the preferred width below. Seeded from before the
   * window was filled, because filling it overwrites the request, and re-read every measure because
   * Compose asks again from a SideEffect of its own.
   */
  private var requestedWidth: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
) : ViewGroup(context) {
  private val locationOnScreen = IntArray(2)
  private val screenBounds = RectF()
  private val screenPath = Path()

  private val dimPaint = Paint().apply {
    color = Color.argb((dimAmount * 255).roundToInt(), 0, 0, 0)
  }

  init {
    // What Stage consumes for the main content: a window filling the host's frame is handed the
    // host's insets in full, and the device inside has none of that hardware.
    //
    // Both paths, because the keyboard travels down two. Compose reads the animated ones from a
    // WindowInsetsAnimation callback, which is dispatched to a subtree on its own and does not pass
    // through what a parent consumed: without stopping that as well, a dialog is handed the host's
    // IME and pads itself out of the way of a keyboard that is not on its screen.
    if (window != null) {
      ViewCompat.setOnApplyWindowInsetsListener(this) { _, _ -> WindowInsetsCompat.CONSUMED }
      ViewCompat.setWindowInsetsAnimationCallback(
        this,
        object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
          override fun onProgress(
            insets: WindowInsetsCompat,
            runningAnimations: MutableList<WindowInsetsAnimationCompat>,
          ): WindowInsetsCompat = insets
        },
      )
    }
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val child = getChildAt(0)
    if (child == null) {
      setMeasuredDimension(0, 0)
      return
    }
    if (!geometry.isReady) {
      child.measure(widthMeasureSpec, heightMeasureSpec)
      setMeasuredDimension(child.measuredWidth, child.measuredHeight)
      return
    }

    if (window != null) requestedWidth = window.attributes.width
    measureContent(child)

    if (window != null) {
      // From the screen, never from the space the window offers: a window that wraps its content
      // sizes itself to what this reports, which would shrink the next offer, and the report with
      // it, until the dialog is cut off mid-screen.
      setMeasuredDimension(
        screenOrigin(0).roundToInt() + geometry.scaledWidth,
        screenOrigin(1).roundToInt() + geometry.scaledHeight,
      )
    } else {
      setMeasuredDimension(
        (child.measuredWidth * geometry.scale).roundToInt(),
        (child.measuredHeight * geometry.scale).roundToInt(),
      )
    }
  }

  /**
   * Measures the window's content against the simulated screen instead of the host's.
   *
   * A dialog that wraps its content does not get the whole screen to wrap into. ViewRootImpl
   * measures it at `config_prefDialogWidth` first — 320dp on a phone — and only widens if the
   * content reports MEASURED_STATE_TOO_SMALL, which is the entire reason an AlertDialog has margins
   * instead of running edge to edge. Filling the window takes that pass away, so the same three
   * steps run here against the simulated screen: measured on a phone at 320dpi, the real device
   * hands the dialog's text 544px and so does this.
   *
   * Only a window that asked to wrap gets it, as in ViewRootImpl.
   */
  private fun measureContent(child: View) {
    val heightSpec = MeasureSpec.makeMeasureSpec(geometry.heightPx, MeasureSpec.AT_MOST)

    // AT_MOST, not EXACTLY: content that wraps has to keep wrapping.
    fun measureAt(width: Int) =
      child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), heightSpec)

    val preferred = preferredDialogWidth()
    if (window == null ||
      requestedWidth != ViewGroup.LayoutParams.WRAP_CONTENT ||
      preferred <= 0 ||
      preferred >= geometry.widthPx
    ) {
      measureAt(geometry.widthPx)
      return
    }

    measureAt(preferred)
    if (!child.tooNarrow) return

    measureAt((preferred + geometry.widthPx) / 2)
    if (!child.tooNarrow) return

    measureAt(geometry.widthPx)
  }

  private val View.tooNarrow: Boolean
    get() = measuredWidthAndState and MEASURED_STATE_TOO_SMALL != 0

  /**
   * The platform's preferred dialog width, in the simulated screen's pixels.
   *
   * config_prefDialogWidth is a framework resource with no public id, so it is looked up by name;
   * it resolves against this Context's DisplayMetrics, which carry the simulated density, so a
   * device at 320dpi reads the platform's 320dp as its own 640px.
   */
  private fun preferredDialogWidth(): Int {
    val id = resources.getIdentifier("config_prefDialogWidth", "dimen", "android")
    if (id == 0) return 0
    return runCatching { resources.getDimensionPixelSize(id) }.getOrDefault(0)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    val child = getChildAt(0) ?: return
    val scale = if (geometry.isReady) geometry.scale else 1f
    val width = child.measuredWidth
    val height = child.measuredHeight
    val left = screenOrigin(0)
    val top = screenOrigin(1)

    child.layout(0, 0, width, height)
    child.pivotX = 0f
    child.pivotY = 0f
    child.scaleX = scale
    child.scaleY = scale
    child.translationX = left + offsetFor(horizontalGravity(), geometry.widthPx, width) * scale
    child.translationY = top + offsetFor(verticalGravity(), geometry.heightPx, height) * scale

    screenBounds.set(left, top, left + geometry.scaledWidth, top + geometry.scaledHeight)
    val corner = geometry.cornerRadiusPx * scale
    screenPath.reset()
    screenPath.addRoundRect(screenBounds, corner, corner, Path.Direction.CW)
  }

  /** Where the simulated screen starts, in this window's own coordinates. */
  private fun screenOrigin(axis: Int): Float {
    if (window == null || !geometry.isReady) return 0f
    getLocationOnScreen(locationOnScreen)
    val screen = if (axis == 0) geometry.screenLeft else geometry.screenTop
    return (screen - locationOnScreen[axis]).toFloat()
  }

  private fun horizontalGravity(): Int =
    Gravity.getAbsoluteGravity(contentGravity, layoutDirection) and Gravity.HORIZONTAL_GRAVITY_MASK

  private fun verticalGravity(): Int = contentGravity and Gravity.VERTICAL_GRAVITY_MASK

  private fun offsetFor(gravity: Int, available: Int, taken: Int): Int {
    if (window == null || !geometry.isReady) return 0
    return when (gravity) {
      Gravity.CENTER_HORIZONTAL, Gravity.CENTER_VERTICAL -> (available - taken) / 2
      Gravity.RIGHT, Gravity.BOTTOM -> available - taken
      else -> 0
    }
  }

  override fun dispatchDraw(canvas: Canvas) {
    if (window == null || !geometry.isReady) {
      super.dispatchDraw(canvas)
      return
    }
    // DeviceFrame's clip is in the composition and cannot reach a window drawn over it.
    val saved = canvas.save()
    canvas.clipPath(screenPath)
    // FLAG_DIM_BEHIND is a full-screen layer, which would grey out the host and the panel with it.
    if (dimAmount > 0f) canvas.drawRect(screenBounds, dimPaint)
    super.dispatchDraw(canvas)
    canvas.restoreToCount(saved)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    val child = getChildAt(0) ?: return false
    // A popup only ever sees what its child did not take, so forwarding untransformed is correct.
    if (window == null) return child.dispatchTouchEvent(event)
    // Dismiss-on-tap-outside is Compose's own, and Dialog.cancel() is overridden to do nothing, so
    // the platform's route is a dead end. Scaling and moving the content invalidates the bounds
    // check in DialogWrapper.onTouchEvent but nothing else, so answer that here and forward a copy
    // that lands nowhere near the content — dismissOnClickOutside still decides.
    if (isInsideContent(event)) return false
    val dialog = window.callback as? Dialog ?: return false
    return when (event.actionMasked) {
      MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        val outside = MotionEvent.obtain(event).apply { setLocation(-1f, -1f) }
        val handled = dialog.onTouchEvent(outside)
        outside.recycle()
        handled
      }

      else -> false
    }
  }

  private fun isInsideContent(event: MotionEvent): Boolean {
    val child = getChildAt(0) ?: return false
    val left = child.translationX
    val top = child.translationY
    val right = left + child.measuredWidth * child.scaleX
    val bottom = top + child.measuredHeight * child.scaleY
    return event.x >= left && event.x < right && event.y >= top && event.y < bottom
  }

  override fun addView(child: View, index: Int, params: LayoutParams?) {
    check(childCount == 0) { "SimulatedWindowLayout hosts exactly one window content view" }
    super.addView(child, index, params)
  }
}
