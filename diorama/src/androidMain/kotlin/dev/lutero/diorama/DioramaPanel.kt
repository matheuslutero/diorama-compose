package dev.lutero.diorama

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.lutero.diorama.frame.DeviceSpec
import dev.lutero.diorama.frame.Devices
import dev.lutero.diorama.frame.Orientation

// TODO(tools): make the sections pluggable — `tools: List<DioramaTool>` composed into this column,
//   so extensions (screenshot capture being the obvious first one) can add UI without this file
//   knowing about them.
@Composable
internal fun DioramaPanel(state: DioramaState, modifier: Modifier = Modifier) {
  Column(
    modifier
      .verticalScroll(rememberScrollState())
      .navigationBarsPadding()
      .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    SectionTitle("Device")
    Devices.All.forEach { spec ->
      DeviceRow(spec, selected = spec.id == state.device.id) {
        state.device = spec
        if (!spec.canRotate) state.orientation = Orientation.Portrait
      }
    }

    Divider()

    SectionTitle("Layout")
    ToggleRow("Show frame", state.isFrameVisible) { state.isFrameVisible = it }
    ToggleRow(
      label = "Landscape",
      checked = state.orientation == Orientation.Landscape,
      enabled = state.device.canRotate,
    ) { state.rotate() }

    Divider()

    SectionTitle("System")
    ToggleRow("Dark mode", state.darkMode) { state.darkMode = it }
    Text("Font scale  ${formatScale(state.fontScale)}", style = MaterialTheme.typography.bodySmall)
    Slider(
      value = state.fontScale,
      onValueChange = { state.fontScale = it },
      valueRange = 0.85f..2f,
    )
  }
}

@Composable
private fun Divider() = HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

@Composable
private fun SectionTitle(text: String) {
  Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = Color.White.copy(alpha = 0.5f),
  )
}

@Composable
private fun DeviceRow(spec: DeviceSpec, selected: Boolean, onClick: () -> Unit) {
  val size = spec.screenSize
  Column(
    Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp)
  ) {
    Text(
      spec.name,
      style = MaterialTheme.typography.bodyMedium,
      color = if (selected) Color(0xFF7AA2F7) else Color.White,
      fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
    )
    Text(
      "${size.width.value.toInt()} x ${size.height.value.toInt()} dp  ·  ${spec.dpi} dpi",
      style = MaterialTheme.typography.labelSmall,
      color = Color.White.copy(alpha = 0.5f),
    )
  }
}

@Composable
private fun ToggleRow(
  label: String,
  checked: Boolean,
  enabled: Boolean = true,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.bodyMedium,
      color = if (enabled) Color.White else Color.White.copy(alpha = 0.3f),
    )
    Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
  }
}

private fun formatScale(value: Float): String {
  val rounded = (value * 100).toInt()
  return "${rounded / 100}.${(rounded % 100).toString().padStart(2, '0')}x"
}
