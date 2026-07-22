package io.github.matheuslutero.diorama

import android.content.Context
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.compose.ui.window.DialogWindowProvider
import org.xmlpull.v1.XmlPullParser
import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * The Context the simulated app sees.
 *
 * It carries the overridden Configuration, and it owns the only two seams Android leaves for
 * anything that opens a window of its own. Both are reached through `LocalView.current.context`,
 * which is why the simulation has to hand the app a View of its own — see [SimulatedWindows].
 *
 * - **WindowManager**, for a Popup: `PopupLayout` asks this Context for it and adds itself.
 * - **LayoutInflater**, for a Dialog: `android.app.Dialog` builds its PhoneWindow from this Context,
 *   and the window inflates its decor through the inflater it finds here. That is the earliest
 *   point at which the dialog is reachable from outside.
 *
 * A fresh instance per distinct Configuration is mandatory: ContextThemeWrapper throws if
 * applyOverrideConfiguration is called twice on the same instance. [geometry] therefore comes from
 * outside — it outlives any one device, and a window already on screen when the device changes goes
 * on reading the same object.
 */
internal class DioramaContext(
  base: Context,
  configuration: Configuration,
  private val geometry: SimulatedWindowGeometry,
) : ContextThemeWrapper(base, 0) {
  private val simulatedWindowManager: WindowManager by lazy {
    val real = super.getSystemService(WINDOW_SERVICE) as WindowManager
    SimulatedWindowManager(real, geometry).asWindowManager()
  }
  private var simulatedInflater: LayoutInflater? = null

  init {
    applyOverrideConfiguration(configuration)
  }

  override fun getSystemService(name: String): Any? = when {
    name == WINDOW_SERVICE && !isDialogUnderConstruction() -> simulatedWindowManager
    name == LAYOUT_INFLATER_SERVICE -> simulatedInflater ?: SimulatedInflater(
      original = super.getSystemService(name) as LayoutInflater,
      context = this,
      onInflated = ::watchForDialogContent,
    ).also { simulatedInflater = it }
    else -> super.getSystemService(name)
  }

  /**
   * android.view.Window.setWindowManager casts whatever it is handed straight to WindowManagerImpl,
   * so handing a Dialog anything else is an immediate ClassCastException. A dialog is re-hosted
   * through the inflater below instead, and must go on seeing the real thing.
   */
  private fun isDialogUnderConstruction(): Boolean =
    Thread.currentThread().stackTrace.any {
      it.className == "android.app.Dialog" && it.methodName == "<init>"
    }

  /**
   * A window's decor is inflated before its content is set, so the content view — the one thing
   * that knows the Window — is not there yet. Wait for it.
   */
  private fun watchForDialogContent(inflated: View) {
    val content = inflated.findViewById<ViewGroup>(android.R.id.content) ?: return
    if (content.childCount > 0) return
    content.setOnHierarchyChangeListener(
      object : ViewGroup.OnHierarchyChangeListener {
        override fun onChildViewAdded(parent: View, child: View) {
          val window = (child as? DialogWindowProvider)?.window ?: return
          content.setOnHierarchyChangeListener(null)
          reHost(content, child, window)
        }

        override fun onChildViewRemoved(parent: View, child: View) = Unit
      },
    )
  }

  /** Puts the simulated screen between the window and its content. */
  private fun reHost(content: ViewGroup, child: View, window: Window) {
    // window.attributes hands back the live object, so read before anything below writes.
    val attrs = window.attributes
    val contentGravity = attrs.gravity
    val requestedWidth = attrs.width
    val dimAmount =
      if (attrs.flags and WindowManager.LayoutParams.FLAG_DIM_BEHIND != 0) attrs.dimAmount else 0f
    // A full-screen layer behind the window; the simulated screen draws its own instead.
    window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)

    // Filled once, before it is shown, so its bounds never change again — a resized surface is
    // stretched across its new bounds until the next buffer arrives.
    window.setGravity(Gravity.TOP or Gravity.LEFT)
    val filling = window.attributes
    filling.x = 0
    filling.y = 0
    filling.width = ViewGroup.LayoutParams.MATCH_PARENT
    filling.height = ViewGroup.LayoutParams.MATCH_PARENT
    window.attributes = filling

    val host =
      SimulatedWindowLayout(content.context, geometry, window, contentGravity, dimAmount, requestedWidth)
    val params = child.layoutParams
    // AbstractComposeView creates its composition on attach and the decor is still detached, so
    // moving the content view costs nothing here.
    content.removeView(child)
    host.addView(child, ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
    content.addView(host, params)
  }

  private companion object {
    const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
  }
}

/**
 * Wraps every window the app adds by hand — Popup, and anything else built on an AbstractComposeView
 * — in a [SimulatedWindowLayout], so it is drawn at the simulation's scale.
 *
 * The position is left alone: Compose computes a popup's x/y from `positionInWindow`, which already
 * runs through the viewport's layer, so the anchor was right and only the size was not.
 *
 * A reflection Proxy rather than an implementation, because WindowManager carries default methods —
 * getCurrentWindowMetrics() and its neighbours throw in the interface itself — and Kotlin's `by`
 * delegation does not forward those, so androidx.window walks into the throwing body.
 */
private class SimulatedWindowManager(
  private val delegate: WindowManager,
  private val geometry: SimulatedWindowGeometry,
) : InvocationHandler {
  private val hosts = mutableMapOf<View, SimulatedWindowLayout>()

  fun asWindowManager(): WindowManager =
    Proxy.newProxyInstance(
      WindowManager::class.java.classLoader,
      arrayOf(WindowManager::class.java),
      this,
    ) as WindowManager

  override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
    val arguments = args ?: emptyArray()
    when {
      method.name == "addView" && arguments.size == 2 -> {
        addView(arguments[0] as View, arguments[1] as ViewGroup.LayoutParams)
        return null
      }

      method.name == "updateViewLayout" && arguments.size == 2 -> {
        delegate.updateViewLayout(hostOf(arguments[0] as View), arguments[1] as ViewGroup.LayoutParams)
        return null
      }

      method.name == "removeView" && arguments.size == 1 -> {
        delegate.removeView(detach(arguments[0] as View))
        return null
      }

      method.name == "removeViewImmediate" && arguments.size == 1 -> {
        delegate.removeViewImmediate(detach(arguments[0] as View))
        return null
      }
    }
    return try {
      method.invoke(delegate, *arguments)
    } catch (e: InvocationTargetException) {
      throw e.cause ?: e
    }
  }

  private fun addView(view: View, params: ViewGroup.LayoutParams) {
    // Wrapped even before the geometry is known — a menu expanded on the first composition shows
    // before the stage has been laid out once. The layout passes its content through until the
    // geometry arrives, where adding the view unwrapped would leave it outside the simulation for
    // as long as it lives.
    val host = SimulatedWindowLayout(view.context, geometry)
    host.addView(view, ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT))
    hosts[view] = host
    delegate.addView(host, params)
  }

  private fun hostOf(view: View): View = hosts[view] ?: view

  private fun detach(view: View): View = hosts.remove(view) ?: view

  private companion object {
    const val WRAP_CONTENT = ViewGroup.LayoutParams.WRAP_CONTENT
  }
}

/**
 * A LayoutInflater that reports what it inflated.
 *
 * Every other inflate overload funnels into this one, and cloneInContext has to return the same
 * kind of inflater or the hook is lost the moment a ContextThemeWrapper clones it — which is
 * exactly what a Dialog does with its theme.
 */
private class SimulatedInflater(
  original: LayoutInflater,
  context: Context,
  private val onInflated: (View) -> Unit,
) : LayoutInflater(original, context) {
  override fun cloneInContext(newContext: Context): LayoutInflater =
    SimulatedInflater(this, newContext, onInflated)

  override fun inflate(parser: XmlPullParser, root: ViewGroup?, attachToRoot: Boolean): View {
    val view = super.inflate(parser, root, attachToRoot)
    onInflated(view)
    return view
  }
}
