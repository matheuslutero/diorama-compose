package dev.lutero.diorama.frame

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CustomDeviceTest {
  @Test
  fun `nearestDensity snaps to the closest bucket`() {
    assertEquals(280, CustomDevice.nearestDensity(271)) // 31 to 240, 9 to 280
    assertEquals(213, CustomDevice.nearestDensity(200)) // 40 to 160, 13 to 213
    assertEquals(640, CustomDevice.nearestDensity(1000)) // clamps to the top bucket
    assertEquals(120, CustomDevice.nearestDensity(0))
  }

  @Test
  fun `nearestDensity only ever returns a real bucket`() {
    (0..700 step 7).forEach { dpi ->
      assertTrue(CustomDevice.nearestDensity(dpi) in CustomDevice.Densities)
    }
  }

  @Test
  fun `of clamps width and height into range`() {
    val clamped = CustomDevice.of(width = 50.dp, height = 5000.dp, dpi = 420)
    assertEquals(CustomDevice.WidthRange.start, clamped.screenSize.width)
    assertEquals(CustomDevice.HeightRange.endInclusive, clamped.screenSize.height)
  }

  @Test
  fun `of snaps the density to a bucket`() {
    assertEquals(280, CustomDevice.of(400.dp, 800.dp, dpi = 271).dpi)
  }

  @Test
  fun `of keeps the reserved custom id`() {
    assertEquals(CustomDevice.Id, CustomDevice.of(400.dp, 800.dp, 420).id)
  }
}
