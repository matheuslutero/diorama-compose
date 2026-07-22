# Architecture

How Diorama is put together, and which parts are load-bearing.

## The shape of the problem

Diorama runs your real app inside a simulated device, on one physical device, and lets you swap
devices live. That splits into two jobs that must not be confused:

- **Tell the app it is on a different device.** Override `Configuration`, `Context`, density,
  layout direction and window size for one subtree.
- **Draw it smaller.** Lay the app out at the device's true logical size and scale only its drawing
  to fit the host screen.

The first is Android-specific. The second is pure Compose. That line is the module boundary.

## Modules

```
:diorama-frame   android, jvm, iosArm64, iosSimulatorArm64
                 DeviceSpec, Devices, DeviceViewport, DeviceFrame
                 May not touch android.content.Context.

:diorama         android
                 ConfigurationOverride, DeviceOverride, Diorama, DioramaState,
                 DioramaBar, DioramaPanel
                 Depends on :diorama-frame via api().

:sample          android app
                 A live check: every reading it prints should move when you switch device.
```

## The composition tree

```
Diorama                             entry point
└─ Column
   ├─ Box (weight 1)                shrinks when the drawer opens
   │  └─ Stage                      statusBarsPadding + displayCutoutPadding
   │     └─ DeviceOverride          Context, Configuration, density, layoutDirection, WindowInfo
   │        └─ SimulatedWindows     LocalView; reports the screen's rect and scale
   │           └─ DeviceViewport    Constraints.fixed(devicePx) → placeWithLayer(scale)
   │              └─ DeviceFrame    bezel, in device dp, inside the scaled layer
   │                 └─ app()
   └─ Surface                       the dock; navigationBarsPadding
      ├─ DioramaBar                 always present
      └─ DioramaPanel               AnimatedVisibility, capped at half the height
```

Three things about that tree are deliberate.

**The panel is a sibling, never an ancestor.** Every override lives inside the stage branch, so the
tool's own UI structurally cannot see them. Flutter's device_preview has to bootstrap its own
`Directionality`, `Localizations` and `Navigator` for the same reason, because it sits above the
user's `MaterialApp`. Compose has no "app widget" that owns those, so a sibling composable in a
`Column` is the whole solution.

**The drawer displaces the stage; it never covers it.** Every control in the panel exists to change
the device, so hiding the device while they are in reach defeats the tool. A `ModalBottomSheet` was
tried and removed: it sits over the preview and dims it, so dragging font scale changed something
you could not see. The drawer is an ordinary `Column` sibling instead, and the stage rescales as it
opens.

**The bezel is inside the scaled layer.** `DeviceViewport` measures at `screen + bezel * 2` and
`DeviceFrame` pads the bezel back off, handing the app exactly `screen`. A bezel drawn outside the
layer keeps constant thickness on the host screen, so a preview scaled down turns into a pill of
solid bezel.

**The bar is always present, including when the simulation is off.** It is the only way back in.

## The load-bearing pieces

### `DeviceViewport`: constrain, then scale

```kotlin
val placeable = measurables.first().measure(Constraints.fixed(widthPx, heightPx))
val scale = fitScale(constraints, widthPx, heightPx)
layout((widthPx * scale).roundToInt(), (heightPx * scale).roundToInt()) {
  placeable.placeWithLayer(0, 0) {
    scaleX = scale; scaleY = scale; transformOrigin = TransformOrigin(0f, 0f)
  }
}
```

The app is *constrained* to the logical size and only ever *scaled* visually. `placeWithLayer` is
load-bearing rather than stylistic: it creates a real `OwnedLayer`, so `NodeCoordinator` hit-tests
through the inverse matrix and pointer input maps correctly with no manual transform. The same scale
via `Canvas.scale` has no layer, so pixels move and touch targets stay behind.

Constraints are always in pixels, so `fitScale` compares like with like whatever the density is.

### `ConfigurationOverride`: six locals in lockstep

All six must move together:

`LocalContext` · `LocalConfiguration` · `LocalLayoutDirection` · `LocalDensity` ·
`LocalFontFamilyResolver` · (`LocalProvidableLocaleList`, unreachable)

Overriding `LocalConfiguration` alone changes nothing for resources. `LocalResources` reads it purely
to invalidate, then takes the actual `Resources` from `LocalContext`, so `stringResource` would
re-read and get the original locale straight back. `LocalContext` is what carries the override.

The `remember` calls are not an optimisation. `Configuration` is a mutable Java class and therefore
unstable, so the composable can never skip; `LocalContext` is a *static* CompositionLocal, so a new
Context identity invalidates the whole subtree unconditionally.

### `DeviceOverride`: every axis, explicitly

`Configuration.updateFrom` merges onto the host, so anything left unset inherits it silently. A host
with a 2× accessibility font scale leaks straight into the simulation otherwise. `densityDpi` comes
from the spec, never from a fit ratio.

`WindowInfoOverride` delegates `WindowInfo` and overrides `containerSize`, which is what makes
`currentWindowAdaptiveInfo()` follow the simulation, because it derives the window size from
`LocalWindowInfo.containerSize` divided by `LocalDensity`.

### The windows the app opens

A dialog, a bottom sheet and a dropdown menu are not in the composition. Each is a separate Android
window, anchored to the display, so none of them passed through `DeviceViewport` and none of them
was in the simulation at all: a dialog opened at the host's density, at the host's size, centred on
the physical screen, dimming the panel with it.

Everything hangs off one fact. `Popup` and `Dialog` both capture `LocalView.current` and then reach
for **its Context** — not the composition's. Overriding `LocalContext` reaches neither, which is why
a dialog inherited the host's Configuration even though the app around it did not. So the simulation
hands the app a View of its own: a zero-sized `FrameLayout` built from the overridden Context. It
must be a real `ViewGroup`, because ripple hosts itself in `LocalView`, and it must really be
attached, because a window needs a live token to be shown.

That View is the seam, and it has two sides:

| | |
|---|---|
| `Popup` | takes the **WindowManager** from that Context and adds itself. `SimulatedWindowManager` wraps whatever is added. |
| `Dialog` | builds its PhoneWindow from that Context, and the window inflates its decor through the **LayoutInflater** it finds there. That is the earliest moment a dialog is reachable from outside. |

The inflater hook waits for the decor's content parent to receive the one child that implements
`DialogWindowProvider` — public API, and the only handle on the Window — and puts a
`SimulatedWindowLayout` between the two. Re-parenting is free at that point: the decor is still
detached, so `AbstractComposeView` has not created its composition yet.

Four things about that layout are load-bearing.

**It is a ViewGroup, not a transform on the window's root.** ViewRootImpl hands the root view raw
window coordinates and nothing above it inverts anything, so a scale set there moves the pixels and
leaves the touch targets behind — the same failure `placeWithLayer` avoids in the composition.
`ViewGroup.dispatchTouchEvent` inverts a child's matrix, so one level of nesting is what makes the
scale real.

**The window is pinned to the simulated screen's rectangle, by correction rather than prediction.**
`WindowManager.LayoutParams` offsets are measured from a parent frame whose bounds depend on the
window's flags and the host's insets, so the layout reads back where it actually landed and aims at
an absolute target. Nudging by the error each pass instead keeps nudging while the previous nudge is
in flight, and the window walks off screen. It runs from a pre-draw listener, because moving a
window does not lay its content out again — a correction applied from `onLayout` is the last one
that ever runs.

**The scrim is drawn by the layout, not by the platform.** `FLAG_DIM_BEHIND` is a full-screen layer
behind the window, so it greys out the host and the panel with it. Cleared, and redrawn inside the
device. With `drawRect`, not `drawColor`: a floating window's surface is inflated to hold its
elevation shadow and `drawColor` fills the clip, so the scrim spilled past the screen by the size of
the shadow.

**A dialog that wraps its content is measured the way ViewRootImpl would have.**
`measureHierarchy` tries `config_prefDialogWidth` first — 320dp on a phone — and only widens if the
content comes back `MEASURED_STATE_TOO_SMALL`. That is the whole reason an AlertDialog has margins
rather than running edge to edge, and pinning the window took the pass away. The same three steps
run against the simulated screen instead. Measured on a phone at 320dpi: the real device hands the
dialog's text 544px, and so does this. Without it the same text got 624px and the dialog ran the
full width of the device.

Dismiss-on-tap-outside is Compose's own, and scaling invalidates it: `DialogWrapper.onTouchEvent`
compares the event against the content's untransformed bounds, and `Dialog.cancel()` is overridden
to do nothing, so the platform's route is a dead end. The layout answers the comparison itself and
forwards a copy that lands nowhere near the content, which leaves the decision — and
`dismissOnClickOutside` with it — where it belongs.

The cost is a contract the app has to know about: **`LocalView.current.context` is no longer the
Activity.** It is the overridden Context, whose base is the Activity, so `(context as Activity)`
throws where walking the ContextWrapper chain works. That cast is already unsafe — Compose hands a
Dialog a `ContextThemeWrapper`, so a screen hosted in a bottom sheet breaks on it with or without
the simulation — but the simulation makes it fail on ordinary screens too, which is where it
survives today.

Two things must not be done here. A `Dialog` must never be handed the proxy WindowManager:
`Window.setWindowManager` casts what it is given straight to `WindowManagerImpl`, so it is an
immediate ClassCastException, and dialogs are re-hosted through the inflater instead. And the proxy
is a reflection `Proxy` rather than an implementation, because `WindowManager` carries default
methods — `getCurrentWindowMetrics()` and its neighbours throw in the interface itself — which
Kotlin's `by` delegation does not forward, so `androidx.window` walked straight into the throwing
body.

### The app survives a device switch, not the simulation toggle

The content sits at one call site inside the simulation, so switching devices recomposes around it
and keeps its state. Toggling the simulation off moves the content to a different call site and
remounts it. The two branches deliberately do not share a composition: `Stage` consumes the host's
window insets, and a shared node would carry that consumed value into the unsimulated branch, where
the app's own `safeDrawingPadding` would then collapse to zero. A `movableContentOf` bridge would
preserve state across the toggle too, but there is no way to reset the inherited inset consumption on
the moved node, so state across the toggle is traded for correct insets.

### `DioramaState`: survives rotation

`rememberSaveable` with a `listSaver`. Rotating the host recreates the Activity, and losing the
simulated device on every rotation defeats the tool. `DeviceSpec` is not Parcelable, so the device is
stored by `id`; `initialDevice` joins the lookup so a caller-supplied device outside `Devices.All`
still restores. This covers configuration changes only. Process death needs DataStore.

## What it cannot do

| | Why |
|---|---|
| Fold / posture | `calculatePosture()` reads `WindowInfoTracker.getOrCreate(context)`, which unwraps to the real Activity. The only lever is `WindowInfoTracker.overrideDecorator()`, which is `@RestrictTo(LIBRARY_GROUP)` and process-global. Window *size class* simulates correctly. |
| Simulated insets / notch | The app inside the frame reads the host's insets. See below. |
| Anything native | It is a composable in your process on your host OS. Fonts, rasterization and performance are the host's. |
| Real dpi rendering | Reported dpi drives resource-bucket selection; pixels render at the host's density and are resampled. |
| Global coordinates | `positionInWindow()` returns host-window coordinates while the simulated size says otherwise. Dialogs and popups are inside the device now, but a `PopupPositionProvider` is still handed the host's display bounds and the popup's unscaled size, so a menu decides whether to flip against the host screen rather than the device's. The anchor is right; the threshold is not. |

### Simulated insets, and why they are not here yet

The chrome keeps clear of the *host's* safe area (`statusBarsPadding` on the stage,
`navigationBarsPadding` on the bar), but the app inside the frame still reads the host's insets.

This was built and then reverted, so it is worth knowing the route before rebuilding it. Insets are
the one axis with no CompositionLocal: they resolve `LocalView` to a
`WeakHashMap<View, WindowInsetsHolder>` to the View's listener, so the only way in is a nested
`AndroidView` intercepting `dispatchApplyWindowInsets` and returning `WindowInsetsCompat.CONSUMED`.
It works. Measured on a Pixel emulator with a 52dp status bar, running the reference Phone at 24dp:
the simulated app read 24dp with the override in place and 52dp without.

It came out because without real per-device safe areas the numbers would be invented, and the API
floors make it partly a lie regardless. Three things to know when it goes back in:

- It must wrap the configuration override, not sit inside it. That `AndroidView` is a new Owner, and
  `ProvideCommonCompositionLocals` re-provides `LocalDensity` from its View's Context
  unconditionally, so density has to be established inside it.
- `setDisplayCutout` needs API 29+ and is a *silent* no-op below that. Per-type `setInsets` needs 30+.
- `WindowInsetsHolder.update()` erases insets for hardware the host lacks, and its `setUseTestInsets`
  escape hatch is `internal` and unreachable. Simulating a button nav bar on a gesture-nav host may
  simply not take. Untested.

`DeviceSpec.safeAreas` and `rotatedSafeAreas` exist for this and are currently unread, except that
`rotatedSafeAreas != null` doubles as `canRotate`. That coupling is inherited from device_preview and
should become an explicit flag.

## Prior art worth knowing

- **device_preview** (Flutter, MIT). The design this follows. Read `device_preview/lib/src/` and
  `device_frame/lib/src/frame.dart`. Its catalog is not trustworthy.
  Its author's own verdict, open since 2019: `WidgetsBinding.instance` is static, so the simulation
  can never be scoped to the wrapped app. Compose's CompositionLocals make that a non-problem, so
  Diorama starts above the ceiling he spent years trying to reach.
- **DeviceConfigurationOverride** (`androidx.compose.ui:ui-test`). The same override idea, but
  test-only (it drags in Espresso) and never wired to a runtime panel.
- **Paparazzi** (`DeviceConfig`). The best-shaped device model in the ecosystem, Apache-2.0.
- **KMPDevicePreview**. The only library with this shape. Dead since 2024, and its override layer is
  the half you would throw away: `LocalDensity` only, `fontScale` hardcoded to 1f, no
  `LocalConfiguration`, dark mode requiring the app to read a custom CompositionLocal.
