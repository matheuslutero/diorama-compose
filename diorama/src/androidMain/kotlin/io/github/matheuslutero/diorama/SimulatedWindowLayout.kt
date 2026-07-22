package io.github.matheuslutero.diorama

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.Window
import android.view.WindowManager
import kotlin.math.roundToInt

/** Enough passes to settle the offset, few enough that a window that cannot move gives up. */
private const val MaxPositionCorrections = 8

/**
 * Hosts the content of one Android window as if that window were the simulated screen.
 *
 * The child is measured against the device's pixel size and only its drawing is scaled — the same
 * split [io.github.matheuslutero.diorama.frame.DeviceViewport] makes in the composition, for the
 * same reason. It has to be a real ViewGroup: a scale set on a window's *root* view is never
 * inverted by anything above it, since ViewRootImpl hands the root raw window coordinates. The
 * pixels would move and the touch targets would stay behind. ViewGroup.dispatchTouchEvent inverts a
 * child's matrix, so one level of nesting is what makes the scale real.
 *
 * With a [window] this is a dialog: it covers the whole simulated screen and places its content by
 * [contentGravity], the way the window manager would have. Without one it is a popup, and it wraps
 * its content so the window stays anchored where Compose put it.
 */
@SuppressLint("ViewConstructor")
internal class SimulatedWindowLayout(
  context: Context,
  private val geometry: SimulatedWindowGeometry,
  private val window: Window? = null,
  private val contentGravity: Int = Gravity.CENTER,
  private val dimAmount: Float = 0f,
) : ViewGroup(context) {
  private val locationOnScreen = IntArray(2)
  private val requested = IntArray(4)
  private var corrections = 0

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val child = getChildAt(0)
    if (child == null) {
      setMeasuredDimension(0, 0)
      return
    }
    if (!geometry.isReady) {
      // Nothing to scale into yet; behave like a plain container rather than collapse the window.
      child.measure(widthMeasureSpec, heightMeasureSpec)
      setMeasuredDimension(child.measuredWidth, child.measuredHeight)
      return
    }

    measureContent(child)
    if (window != null) {
      setMeasuredDimension(geometry.scaledWidth, geometry.scaledHeight)
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
   * content reports MEASURED_STATE_TOO_SMALL, which is the entire reason an AlertDialog has
   * margins instead of running edge to edge. Pinning the window to the device's rectangle takes
   * that pass away, because the decor now measures against a size chosen here, so the same three
   * steps run here instead, against the simulated screen. Measured on a phone at 320dpi: the real
   * device hands the dialog's text 544px and so does this.
   *
   * Only a window that asked to wrap gets it, exactly as in ViewRootImpl. Compose sets MATCH_PARENT
   * for a dialog with usePlatformDefaultWidth off — a full-screen destination — and there the
   * platform skips the step too.
   */
  private fun measureContent(child: View) {
    val heightSpec = MeasureSpec.makeMeasureSpec(geometry.heightPx, MeasureSpec.AT_MOST)

    // AT_MOST, not EXACTLY: content that wraps has to keep wrapping, and content that fills asks
    // for the whole bound anyway.
    fun measureAt(width: Int) =
      child.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.AT_MOST), heightSpec)

    val preferred = preferredDialogWidth()
    if (window == null ||
      window.attributes.width != ViewGroup.LayoutParams.WRAP_CONTENT ||
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

    child.layout(0, 0, width, height)
    child.pivotX = 0f
    child.pivotY = 0f
    child.scaleX = scale
    child.scaleY = scale
    child.translationX = offsetFor(horizontalGravity(), geometry.widthPx, width) * scale
    child.translationY = offsetFor(verticalGravity(), geometry.heightPx, height) * scale
  }

  // Moving a window does not lay its content out again, so a correction applied from onLayout is
  // the last one that ever runs. Every traversal reaches a pre-draw.
  private val syncOnPreDraw = ViewTreeObserver.OnPreDrawListener {
    syncWindow()
    true
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()
    if (window != null) viewTreeObserver.addOnPreDrawListener(syncOnPreDraw)
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    if (window != null) viewTreeObserver.removeOnPreDrawListener(syncOnPreDraw)
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

  /**
   * Pins the window to the simulated screen's rectangle.
   *
   * The offsets in WindowManager.LayoutParams are measured from a parent frame whose bounds depend
   * on the window's flags and on the host's insets, so rather than predict that origin this reads
   * it back: wherever the window landed, `locationOnScreen - x` is where its frame starts.
   *
   * The target has to be absolute for the same reason a correction cannot be applied twice. An
   * offset nudged by the error each pass keeps nudging while the previous nudge is still in flight,
   * and the window walks off the screen instead of settling.
   */
  private fun syncWindow() {
    val window = window ?: return
    if (!geometry.isReady || corrections > MaxPositionCorrections) return

    val current = window.attributes
    getLocationOnScreen(locationOnScreen)
    val frameLeft = locationOnScreen[0] - current.x
    val frameTop = locationOnScreen[1] - current.y
    val gravity = Gravity.TOP or Gravity.LEFT
    val x = geometry.screenLeft - frameLeft
    val y = geometry.screenTop - frameTop

    // A window that wraps its content is left wrapping: this layout already measures to the
    // simulated screen, so WRAP_CONTENT resolves to exactly the rectangle it should. Overwriting it
    // with a pixel size would work too, and would also erase the one record of what the window
    // asked for — which is what decides whether the dialog gets the platform's preferred width.
    // MATCH_PARENT has to go, or the window takes the host's display instead.
    val wrap = ViewGroup.LayoutParams.WRAP_CONTENT
    val target = intArrayOf(
      x,
      y,
      if (current.width == wrap) wrap else geometry.scaledWidth,
      if (current.height == wrap) wrap else geometry.scaledHeight,
    )
    val settled = current.gravity == gravity &&
      current.x == target[0] &&
      current.y == target[1] &&
      current.width == target[2] &&
      current.height == target[3]
    if (settled) {
      corrections = 0
      return
    }
    // A moving target — the panel opening, a device switch — always earns a fresh budget; only a
    // window that will not go where it is asked runs out of one.
    if (!target.contentEquals(requested)) {
      corrections = 0
      target.copyInto(requested)
    }

    corrections++
    val updated = WindowManager.LayoutParams().apply {
      copyFrom(current)
      this.gravity = gravity
      this.x = target[0]
      this.y = target[1]
      width = target[2]
      height = target[3]
    }
    post { window.attributes = updated }
  }

  private val dimPaint = Paint().apply {
    color = Color.argb((dimAmount * 255).roundToInt(), 0, 0, 0)
  }

  override fun dispatchDraw(canvas: Canvas) {
    // drawRect, not drawColor: a floating window's surface is inflated to hold its elevation
    // shadow, and drawColor fills the clip rather than the view, so the scrim would spill past the
    // simulated screen by the size of the shadow.
    if (dimAmount > 0f) {
      canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
    }
    super.dispatchDraw(canvas)
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {
    val child = getChildAt(0) ?: return false
    if (window == null) {
      // A popup reads its own bounds to decide it was dismissed; it only ever sees what the child
      // did not take, so forwarding untransformed is correct.
      return child.dispatchTouchEvent(event)
    }
    // Dismiss-on-tap-outside is Compose's own: DialogWrapper.onTouchEvent compares the event
    // against the content's untransformed bounds, and Dialog.cancel() is overridden to do nothing,
    // so the platform's route is a dead end. Scaling the content invalidates that comparison — the
    // rect it measures is in device pixels while the event is in window pixels — but only the
    // comparison. Answering it here and forwarding a copy that lands nowhere near the content
    // leaves the decision, and properties.dismissOnClickOutside with it, where it belongs.
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
