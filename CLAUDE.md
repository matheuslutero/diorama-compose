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
| Tests | none yet |
| Linter / formatter | none configured |

## Build and run

```bash
./gradlew :sample:assembleDebug     # build
./gradlew :sample:installDebug      # build + install
adb shell am start -n dev.lutero.diorama.sample/.MainActivity
```

## Verify on a device, not in the compiler

This is the rule that matters most here. A Compose override that compiles, and even reads back the
value you set, can still be wrong in ways only a running app reveals. Every real bug in this project
so far was caught by running it and reading the sample's output: a bezel that turned into a pill
when scaled down, a panel that starved the stage, `sizeFor()` returning landscape for a portrait
tablet. None of them would have failed a build.

The sample prints every value the simulation is supposed to drive. After a change, run it and check
the readings move. If a claim is worth making in a commit or a doc, measure it first. If the
evidence is from an earlier run, measure it again rather than trusting it.

## Do not invent device metrics

The catalog currently holds Android Studio's four reference specs, which are the only upstream device
definitions that are both authoritative and self-contained. Everything else is a `TODO(catalog)`.

Do not fill gaps by guessing, and do not port device_preview's catalog: its iPhone 12 carries an
iPad Pro screen size, and several entries have half the correct pixel ratio. If a number cannot be
sourced, leave the field out and say so in the TODO. A preview that looks authoritative and lies is
worse than one that is visibly incomplete.

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

`ConfigurationOverride.kt` is derived from AOSP and carries its Apache-2.0 attribution. Keep the
header if you touch it.

## Toolchain quirks worth not rediscovering

- AGP 9 bans `com.android.library` alongside KMP. Library modules use
  `com.android.kotlin.multiplatform.library` with `kotlin { androidLibrary { namespace = ... } }`.
- AGP 9 bans the `org.jetbrains.kotlin.android` plugin, because Kotlin is built in. `:sample` is a
  plain Android app for this reason.
- `androidx.core` 1.18+ requires compileSdk 37, which AGP 9.0.1 does not recommend yet.
- Compose Multiplatform dropped the `iosX64` target.
