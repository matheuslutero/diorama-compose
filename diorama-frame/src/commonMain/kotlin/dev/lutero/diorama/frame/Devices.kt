package dev.lutero.diorama.frame

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * Devices derived from the Android SDK's own definitions, which ship as XML resources inside
 * `cmdline-tools/latest/lib/sdklib/sdklib.core.jar`, under `com.android.sdklib.devices`
 * (Apache-2.0, AOSP). Each entry records the source file, the upstream id, and the physical
 * pixels and density it was computed from, so every number here can be checked against upstream
 * rather than taken on trust.
 *
 * dp is `px / (dpi / 160)` truncated, which is how Android itself derives `screenWidthDp`.
 *
 * There is no `large_phone` upstream: the generic set is `small_phone`, `medium_phone` and
 * `medium_tablet` only. [LargePhone] therefore uses the largest real phone in the catalog rather
 * than an invented specification.
 *
 * TODO(catalog): read these from the `com.android.tools:sdklib` artifact at build time instead of
 *   transcribing them, so new hardware arrives without a manual edit. Two gaps the schema will not
 *   fill either way: screen roundness/chin, which only reaches AOSP via a boot-prop hack, and
 *   cutouts, which exist solely in Studio's DeviceSpec layer and have to be authored by hand.
 */
object Devices {
  // devices.xml, id "small_phone": 720x1280 px @ 320dpi (xhdpi), 4.65"
  val SmallPhone = DeviceSpec(
    id = "small_phone",
    name = "Small Phone",
    screenSize = DpSize(360.dp, 640.dp),
    dpi = 320,
    category = DeviceCategory.Phone,
  )

  // devices.xml, id "medium_phone": 1080x2400 px @ 420dpi, 6.4"
  val MediumPhone = DeviceSpec(
    id = "medium_phone",
    name = "Medium Phone",
    screenSize = DpSize(411.dp, 914.dp),
    dpi = 420,
    category = DeviceCategory.Phone,
  )

  // nexus.xml, id "pixel_9_pro_xl": 1344x2992 px @ 480dpi, 6.8"
  val LargePhone = DeviceSpec(
    id = "pixel_9_pro_xl",
    name = "Large Phone",
    screenSize = DpSize(448.dp, 997.dp),
    dpi = 480,
    category = DeviceCategory.Phone,
  )

  // devices.xml, id "7.6in Foldable": 1768x2208 px @ 420dpi, 7.59". Matches the FOLDABLE
  // reference spec in androidx.compose.ui.tooling.preview.Devices (673x841dp).
  val Foldable = DeviceSpec(
    id = "foldable_7_6in",
    name = "Foldable",
    screenSize = DpSize(673.dp, 841.dp),
    dpi = 420,
    category = DeviceCategory.Foldable,
  )

  // nexus.xml, id "pixel_9_pro_fold": 2076x2152 px @ 390dpi, 8". Unfolded; Diorama does not
  // simulate posture, so the folded region is not modelled.
  val LargeFoldable = DeviceSpec(
    id = "pixel_9_pro_fold",
    name = "Large Foldable",
    screenSize = DpSize(851.dp, 882.dp),
    dpi = 390,
    category = DeviceCategory.Foldable,
  )

  // devices.xml, id "medium_tablet": 2560x1600 px @ 320dpi (xhdpi), 10.05"
  val MediumTablet = DeviceSpec(
    id = "medium_tablet",
    name = "Medium Tablet",
    screenSize = DpSize(1280.dp, 800.dp),
    dpi = 320,
    category = DeviceCategory.Tablet,
  )

  // desktop.xml, id "desktop_small": 1366x768 px @ 160dpi
  val SmallDesktop = DeviceSpec(
    id = "desktop_small",
    name = "Small Desktop",
    screenSize = DpSize(1366.dp, 768.dp),
    dpi = 160,
    category = DeviceCategory.Desktop,
    canRotate = false,
    platform = DevicePlatform.Desktop,
  )

  // desktop.xml, id "desktop_medium": 3840x2160 px @ 320dpi. Same dp as LargeDesktop at twice
  // the density: a 4K panel at 2x scaling against a 1080p panel at 1x.
  val MediumDesktop = DeviceSpec(
    id = "desktop_medium",
    name = "Medium Desktop (HiDPI)",
    screenSize = DpSize(1920.dp, 1080.dp),
    dpi = 320,
    category = DeviceCategory.Desktop,
    canRotate = false,
    platform = DevicePlatform.Desktop,
  )

  // desktop.xml, id "desktop_large": 1920x1080 px @ 160dpi. Matches the DESKTOP reference spec in
  // androidx.compose.ui.tooling.preview.Devices.
  val LargeDesktop = DeviceSpec(
    id = "desktop_large",
    name = "Large Desktop",
    screenSize = DpSize(1920.dp, 1080.dp),
    dpi = 160,
    category = DeviceCategory.Desktop,
    canRotate = false,
    platform = DevicePlatform.Desktop,
  )

  val All: List<DeviceSpec> = listOf(
    SmallPhone,
    MediumPhone,
    LargePhone,
    Foldable,
    LargeFoldable,
    MediumTablet,
    SmallDesktop,
    MediumDesktop,
    LargeDesktop,
  )
}
