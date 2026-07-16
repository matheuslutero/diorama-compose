package io.github.matheuslutero.diorama.frame

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DevicesTest {
  @Test
  fun `catalog is not empty`() {
    assertTrue(Devices.All.isNotEmpty())
  }

  @Test
  fun `ids are unique`() {
    val ids = Devices.All.map { it.id }
    assertEquals(ids.size, ids.toSet().size)
  }

  @Test
  fun `every picker category has at least one device`() {
    val present = Devices.All.map { it.category }.toSet()
    listOf(
      DeviceCategory.Phone,
      DeviceCategory.Foldable,
      DeviceCategory.Tablet,
      DeviceCategory.Desktop,
    ).forEach { assertTrue(it in present, "no device for $it") }
  }

  @Test
  fun `desktops do not rotate`() {
    Devices.All
      .filter { it.category == DeviceCategory.Desktop }
      .forEach { assertTrue(!it.canRotate, "${it.id} should not rotate") }
  }

  @Test
  fun `every device has a positive dpi and size`() {
    Devices.All.forEach {
      assertTrue(it.dpi > 0, "${it.id} dpi")
      assertTrue(it.screenSize.width.value > 0f && it.screenSize.height.value > 0f, "${it.id} size")
    }
  }
}
