package dev.lutero.diorama.frame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws the device bezel around [content], sized in the *device's* dp.
 *
 * This has to live inside the scaled layer, alongside the screen: a bezel drawn outside it keeps a
 * constant thickness on the host screen, so a preview scaled down to thumbnail size turns into a
 * pill of solid bezel. The caller measures at screen size + [bezel] * 2 and scales the whole thing
 * as one unit; padding by [bezel] here is what hands [content] exactly the screen size back.
 *
 * TODO(frame): placeholder rounded rect. Real devices need two facts that must be derived from one
 *   another rather than hand-written separately — the numeric safe areas feeding the app's insets,
 *   and the screen path clipping the notch and corners. device_preview keeps both by hand and they
 *   have drifted apart. Ship the geometry as SVG path data parsed with
 *   androidx.compose.ui.graphics.vector.PathParser, not generated code: device_preview carries
 *   ~640 lines of generated painter per device, which is why its catalog stopped being maintained.
 */
@Composable
fun DeviceFrame(
  bezel: Dp,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit,
) {
  if (bezel <= 0.dp) {
    Box(modifier.fillMaxSize()) { content() }
    return
  }
  Box(
    modifier
      .fillMaxSize()
      .background(Color(0xFF15151A), RoundedCornerShape(bezel * 2.5f))
      .padding(bezel)
      .clip(RoundedCornerShape(bezel * 1.5f))
  ) {
    content()
  }
}
