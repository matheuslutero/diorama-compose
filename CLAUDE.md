# Diorama

A Compose library that runs a real app inside a simulated device, on one physical device, with live
device switching. Inspired by Flutter's `device_preview`.

Read [docs/architecture.md](docs/architecture.md) before changing anything structural.

## Stack

| | |
|---|---|
| Kotlin | 2.4.0 |
| AGP | 9.0.1 |
| Gradle | 9.6.1 (wrapper) |
| Compose Multiplatform | 1.11.1 |
| compileSdk / minSdk | 36 / 24 |
| Tests | kotlin-test, in `diorama-frame` commonTest |
| Linter / formatter | ktlint |

## Build and run

```bash
./gradlew :sample:assembleDebug     # build
./gradlew :sample:installDebug      # build + install
adb shell am start -n io.github.matheuslutero.diorama.sample/.MainActivity
```

## Verify on a device, not in the compiler

The rule that matters most here. A Compose override that compiles, and even reads back the value you
set, can still be wrong in ways only a running app reveals. The insets fix is the latest case: the
probe read `WindowInsets.safeDrawing` as 156px while `safeDrawingPadding` applied 0. Earlier ones: a
bezel that turned into a pill when scaled down, a panel that starved the stage, `sizeFor()` returning
landscape for a portrait tablet. None would have failed a build.

Run the sample after a change and watch the layout react to a device switch: the grid reflows, the
two-pane detail appears on wide screens, the insets change with the simulation on and off. If a claim
is worth making in a commit or a doc, measure it on a device first, and measure it again rather than
trusting an earlier run.

## Invariants

- **Constrain, then scale.** The app is measured at `Constraints.fixed(devicePx)` and only its
  drawing is scaled. Never scale to constrain.
- **`placeWithLayer` / `graphicsLayer`, never `Canvas.scale`.** A draw-time scale has no `OwnedLayer`,
  so pointers do not map through it. Pixels move, touch targets stay behind, nothing errors.
- **`densityDpi` comes from the spec.** Never from a fit ratio. Deriving it makes the simulated dpi a
  function of the preview's layout, which is why `ForcedSize` is unusable here.
- **Every axis explicitly.** `Configuration.updateFrom` merges onto the host, so any axis left unset
  inherits it silently.
- **The panel stays a sibling of the preview, never an ancestor.** That is what keeps the overrides
  out of the tool's own UI.
- **The panel never covers the stage.** Every control in it changes the device; hiding the device
  while they are in reach defeats the tool. It displaces the stage and the device rescales.
- **Offer only values a device can report.** Density steps through the real `DisplayMetrics` buckets
  and font scale through the values Android's Settings offers. A free slider invents 271dpi and
  0.8558874x, which no device produces and no bug report will ever cite.
- **`:diorama-frame` may not touch `android.content.Context`.** That boundary is what keeps it
  portable.
- **The bar stays visible when the simulation is off.** It is the only way back in.

## Code style

Two-space indent. Version catalog for every dependency, with no inline coordinates. Comments explain a
constraint the code cannot show (an API floor, a silent failure mode, a reason an obvious approach is
wrong), not what the next line does.

## Toolchain quirks worth not rediscovering

- AGP 9 bans `com.android.library` alongside KMP. Library modules use
  `com.android.kotlin.multiplatform.library` with `kotlin { androidLibrary { namespace = ... } }`.
- AGP 9 bans the `org.jetbrains.kotlin.android` plugin, because Kotlin is built in. `:sample` is a
  plain Android app for this reason.
- Compose Multiplatform dropped the `iosX64` target.
