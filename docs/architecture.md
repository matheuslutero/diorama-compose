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
Diorama                          entry point
└─ Column
   ├─ Box (weight 1)             shrinks when the drawer opens
   │  └─ Stage                   statusBarsPadding + displayCutoutPadding
   │     └─ DeviceOverride       Context, Configuration, density, layoutDirection, WindowInfo
   │        └─ DeviceViewport    Constraints.fixed(devicePx) → placeWithLayer(scale)
   │           └─ DeviceFrame    bezel, in device dp, inside the scaled layer
   │              └─ app()
   └─ Surface                    the dock; navigationBarsPadding
      ├─ DioramaBar              always present
      └─ DioramaPanel            AnimatedVisibility, capped at half the height
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

Vendored from AOSP with attribution. All six must move together:

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
| Global coordinates | `positionInWindow()` returns host-window coordinates while the simulated size says otherwise. Every dropdown and dialog bug in device_preview's tracker is this. |

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
- **DeviceConfigurationOverride** (`androidx.compose.ui:ui-test`). The engine, vendored here.
- **Paparazzi** (`DeviceConfig`). The best-shaped device model in the ecosystem, Apache-2.0.
- **KMPDevicePreview**. The only library with this shape. Dead since 2024, and its override layer is
  the half you would throw away: `LocalDensity` only, `fontScale` hardcoded to 1f, no
  `LocalConfiguration`, dark mode requiring the app to read a custom CompositionLocal.
