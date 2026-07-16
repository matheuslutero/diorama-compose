<div align="center">
  <img src="docs/art.png" width="120" alt="Diorama logo" />
  <h1>Diorama</h1>
  <p><b>Run your Compose app inside a simulated device and swap the hardware live. One real phone is enough.</b></p>
  <p>
    <img src="https://img.shields.io/badge/Android-3DDC84?logo=android&logoColor=white" alt="Android" />
    <img src="https://img.shields.io/badge/Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License" />
  </p>
  <br />
  <img src="docs/preview.gif" width="300" alt="Diorama in action" />
</div>

A miniature, faithful model of a device holding your real, interactive app. Inspired by Flutter's
[`device_preview`](https://pub.dev/packages/device_preview).

## Quickstart

```kotlin
setContent {
  Diorama {
    MyApp()
  }
}
```

Wrap your root and a panel lets you change the simulated device, orientation, font scale and dark
mode while the app keeps running.

> [!WARNING]
> Early skeleton. The override engine and the scaling primitive are real and verified; the device
> catalog and the bezel art are placeholders.

## Why

Android Studio's `@Preview` is design-time and per-composable. Showkase and Composium are component
browsers. Paparazzi and Roborazzi render off-device for snapshot tests. Commercial options are cloud
device farms or CI visual-diffs. None run your live app inside a simulated device on the phone in
your hand.

The correct half, a configuration-override engine, already ships as
`androidx.compose.ui.test.DeviceConfigurationOverride`. Diorama wires it to a runtime panel.

## How it works

| Module | Targets | Holds |
|---|---|---|
| `:diorama-frame` | android, jvm, iosArm64, iosSimulatorArm64 | Device model, catalog, bezel, scaling primitive. Never touches `Context`. |
| `:diorama` | android | Configuration-override engine, panel, state. |

The app is constrained to the device's logical size and only scaled visually, so it reports the size
it lays out at. The override sets every axis explicitly (`densityDpi`, `screenWidthDp`, font scale)
instead of deriving density from a fit ratio the way `ForcedSize` does.

Simulating a 1280×800 tablet at 240dpi on a Pixel emulator:

| | `windowSizeClass` | `screenWidthDp` | `densityDpi` |
|---|---|---|---|
| `ForcedSize` | minW=840, minH=900 *(wrong height)* | 427 *(host's)* | 159 *(wrong)* |
| Diorama | minW=840, minH=480 | 1280 | 240 |

Only Diorama reports the real device. Full write-up: [docs/architecture.md](docs/architecture.md).

## What it cannot do

- **Fold / posture.** `WindowInfoTracker` unwraps to the real Activity, so an overridden Context
  cannot fool it. Window size class simulates; posture does not.
- **Anything native.** It is a composable in your process on your host OS, so fonts, rasterization
  and performance are the host's.
- **Real dpi rendering.** The reported dpi drives resource-bucket selection, but pixels draw at the
  host's density and resample.
- **Global coordinates.** `positionInWindow()` returns host-window coordinates, so dropdowns and
  popups can misplace.

<details>
<summary><b>Roadmap</b></summary>

- SVG-path bezels with per-device safe areas and notch.
- Simulated insets for the app inside the frame (it still reads the host's).
- Locale override, settings persistence, a pluggable tools API (screenshot capture first).
- Other platforms: `:diorama-frame` is portable; `:diorama` needs an expect/actual seam for the
  Context/Configuration half.

</details>
