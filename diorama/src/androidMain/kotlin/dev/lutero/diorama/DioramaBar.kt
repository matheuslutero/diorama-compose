package dev.lutero.diorama

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Always present, including with the simulation off, because it is the only way back in.
 *
 * Draws no background of its own: the drawer around it owns the surface, so the bar and the settings
 * below it read as one panel rather than two stacked ones.
 */
@Composable
internal fun DioramaBar(state: DioramaState) {
  val highlight by animateFloatAsState(
    if (state.isPanelOpen) 0.16f else 0f,
    label = "settingsHighlight",
  )

  Row(
    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(
      Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF7AA2F7).copy(alpha = highlight))
        .clickable { state.isPanelOpen = !state.isPanelOpen }
        .padding(8.dp),
    ) {
      TuneIcon(if (state.isPanelOpen) Color(0xFF7AA2F7) else Color(0xFFECECF1))
    }

    Text(
      "Device Preview",
      modifier = Modifier.weight(1f).padding(start = 10.dp),
      style = MaterialTheme.typography.bodyMedium,
      color = Color(0xFFECECF1),
    )

    Switch(
      checked = state.isEnabled,
      onCheckedChange = { state.isEnabled = it },
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Color(0xFF7AA2F7),
        uncheckedTrackColor = Color(0xFF22222B),
        uncheckedBorderColor = Color.White.copy(alpha = 0.08f),
      ),
    )
  }
}

/** Drawn rather than imported, to keep this module off the material-icons artifacts. */
@Composable
private fun TuneIcon(tint: Color) {
  Canvas(Modifier.size(20.dp)) {
    val stroke = 1.6.dp.toPx()
    val knobRadius = 2.6.dp.toPx()
    val rails = listOf(0.22f to 0.66f, 0.5f to 0.34f, 0.78f to 0.56f)

    rails.forEach { (yFraction, knobFraction) ->
      val y = size.height * yFraction
      val knobX = size.width * knobFraction
      drawLine(tint, Offset(0f, y), Offset(size.width, y), stroke, StrokeCap.Round)
      // Punch the rail out behind the knob so the glyph reads as a slider, not a crossed circle.
      drawCircle(PanelBackground, knobRadius + stroke, Offset(knobX, y))
      drawCircle(tint, knobRadius, Offset(knobX, y), style = Stroke(stroke))
    }
  }
}
