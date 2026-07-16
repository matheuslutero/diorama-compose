package io.github.matheuslutero.diorama.frame

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceSpecTest {
  private fun spec(
    size: DpSize,
    dpi: Int = 420,
    canRotate: Boolean = true,
    safeAreas: DeviceInsets = DeviceInsets(),
    rotatedSafeAreas: DeviceInsets? = null,
  ) = DeviceSpec(
    id = "test",
    name = "Test",
    screenSize = size,
    dpi = dpi,
    canRotate = canRotate,
    safeAreas = safeAreas,
    rotatedSafeAreas = rotatedSafeAreas,
  )

  @Test
  fun `density is dpi over 160`() {
    assertEquals(2.625f, spec(DpSize(411.dp, 914.dp), dpi = 420).density)
    assertEquals(1f, spec(DpSize(360.dp, 640.dp), dpi = 160).density)
  }

  @Test
  fun `sizeFor normalises a landscape-native size to short-by-long for portrait`() {
    val tablet = spec(DpSize(1280.dp, 800.dp))
    assertEquals(DpSize(800.dp, 1280.dp), tablet.sizeFor(Orientation.Portrait))
    assertEquals(DpSize(1280.dp, 800.dp), tablet.sizeFor(Orientation.Landscape))
  }

  @Test
  fun `a non-rotatable device keeps its size in both orientations`() {
    val monitor = spec(DpSize(1366.dp, 768.dp), canRotate = false)
    assertEquals(DpSize(1366.dp, 768.dp), monitor.sizeFor(Orientation.Portrait))
    assertEquals(DpSize(1366.dp, 768.dp), monitor.sizeFor(Orientation.Landscape))
  }

  @Test
  fun `safeAreasFor uses the rotated insets only in landscape`() {
    val portrait = DeviceInsets(top = 24.dp)
    val landscape = DeviceInsets(left = 24.dp)
    val phone = spec(DpSize(411.dp, 914.dp), safeAreas = portrait, rotatedSafeAreas = landscape)
    assertEquals(portrait, phone.safeAreasFor(Orientation.Portrait))
    assertEquals(landscape, phone.safeAreasFor(Orientation.Landscape))
  }

  @Test
  fun `safeAreasFor falls back to the portrait insets when none are set for landscape`() {
    val insets = DeviceInsets(top = 24.dp)
    val phone = spec(DpSize(411.dp, 914.dp), safeAreas = insets, rotatedSafeAreas = null)
    assertEquals(insets, phone.safeAreasFor(Orientation.Landscape))
  }
}
