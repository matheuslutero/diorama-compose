package dev.lutero.diorama.frame

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Starter catalog: Android Studio's four reference devices, from
 * androidx.compose.ui.tooling.preview.Devices combined with Preview.DeviceSpec.DEFAULT_DPI (420).
 * These are specifications rather than real hardware, which is deliberate — they are the only
 * device definitions upstream that are both authoritative and self-contained. The `Devices`
 * constants for real hardware are opaque ids ("id:pixel_7") that only resolve against Studio's
 * own device database.
 *
 * TODO(catalog): import real hardware from the com.android.tools:sdklib artifact (Apache-2.0),
 *   which carries devices.xml/nexus.xml/wear.xml with real dpi and foldable regions. Two gaps it
 *   will not fill: screen roundness/chin, and cutouts — `cutout` exists only in Studio's
 *   DeviceSpec layer, not the AOSP schema, so it has to be hand-authored.
 *   Do not port device_preview's catalog: its iPhone 12 is defined with an iPad Pro screen size,
 *   and several 2020-21 entries carry half the correct pixel ratio.
 */
object Devices {
  val Phone = DeviceSpec(
    id = "reference_phone",
    name = "Phone",
    screenSize = DpSize(411.dp, 891.dp),
    dpi = 420,
    rotatedSafeAreas = DeviceInsets(),
  )

  val Foldable = DeviceSpec(
    id = "reference_foldable",
    name = "Foldable",
    screenSize = DpSize(673.dp, 841.dp),
    dpi = 420,
    rotatedSafeAreas = DeviceInsets(),
  )

  val Tablet = DeviceSpec(
    id = "reference_tablet",
    name = "Tablet",
    screenSize = DpSize(1280.dp, 800.dp),
    dpi = 240,
    rotatedSafeAreas = DeviceInsets(),
  )

  val Desktop = DeviceSpec(
    id = "reference_desktop",
    name = "Desktop",
    screenSize = DpSize(1920.dp, 1080.dp),
    dpi = 160,
    platform = DevicePlatform.Desktop,
  )

  val All: List<DeviceSpec> = listOf(Phone, Foldable, Tablet, Desktop)
}
