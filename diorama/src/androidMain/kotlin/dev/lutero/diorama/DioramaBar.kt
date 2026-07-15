package dev.lutero.diorama

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

private val BarBackground = Color(0xFF1B1B20)

/**
 * Always-present entry point: the app keeps the whole screen above it, and this stays visible even
 * with the simulation off — it is the only way back in.
 */
@Composable
internal fun DioramaBar(state: DioramaState) {
  Surface(color = BarBackground, contentColor = Color.White) {
    Row(
      Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 8.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = { state.isPanelOpen = true }) { TuneIcon() }
      Text(
        "Device Preview",
        modifier = Modifier.weight(1f).padding(start = 4.dp),
        style = MaterialTheme.typography.bodyMedium,
      )
      Switch(checked = state.isEnabled, onCheckedChange = { state.isEnabled = it })
    }
  }
}

/** Drawn rather than imported to keep this module off the material-icons artifacts. */
@Composable
private fun TuneIcon(tint: Color = Color.White) {
  Canvas(Modifier.size(22.dp)) {
    val stroke = 1.6.dp.toPx()
    val knobRadius = 2.6.dp.toPx()
    val rails = listOf(0.22f to 0.66f, 0.5f to 0.34f, 0.78f to 0.56f)

    rails.forEach { (yFraction, knobFraction) ->
      val y = size.height * yFraction
      val knobX = size.width * knobFraction
      drawLine(tint, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
      // Punch the rail out behind the knob so the glyph reads as a slider, not a crossed circle.
      drawCircle(BarBackground, knobRadius + stroke, Offset(knobX, y))
      drawCircle(tint, knobRadius, Offset(knobX, y), style = Stroke(stroke))
    }
  }
}
