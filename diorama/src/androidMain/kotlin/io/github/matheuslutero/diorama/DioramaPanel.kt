package io.github.matheuslutero.diorama

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.matheuslutero.diorama.frame.CustomDevice
import io.github.matheuslutero.diorama.frame.DeviceCategory
import io.github.matheuslutero.diorama.frame.Orientation
import kotlin.math.roundToInt

private val Signal = Color(0xFF7AA2F7)
private val Ink = Color(0xFFECECF1)
private val Muted = Color(0xFF8A8A99)
private val Raised = Color(0xFF22222B)
private val Line = Color.White.copy(alpha = 0.08f)

// Raised disappears against PanelBackground as a slider track.
private val TrackIdle = Color.White.copy(alpha = 0.14f)

private val EdgeInset = 16.dp

/** The font scales Android's own Settings offers. */
private val FontScales = listOf(0.85f, 1f, 1.15f, 1.3f, 1.5f, 1.8f, 2f)

// TODO(tools): make the sections pluggable via `tools: List<DioramaTool>` composed into this column,
//   so extensions (screenshot capture being the obvious first one) can add UI without this file
//   knowing about them.
//
// The horizontal inset lives on each section, not on the column: the chip rows scroll edge to edge.
@Composable
internal fun DioramaPanel(state: DioramaState, modifier: Modifier = Modifier) {
  val inset = Modifier.padding(horizontal = EdgeInset)
  Column(
    modifier
      .verticalScroll(rememberScrollState())
      .padding(bottom = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Label("Device", inset)
    CategoryRow(state)
    DeviceRow(state)
    SpecReadout(state, inset)

    Divider(inset)

    Label("Layout", inset)
    ToggleRow("Device frame", state.isFrameVisible, modifier = inset) { state.isFrameVisible = it }
    ToggleRow(
      label = "Landscape",
      checked = state.orientation == Orientation.Landscape,
      enabled = state.device.canRotate,
      modifier = inset,
    ) { state.rotate() }

    Divider(inset)

    Label("System", inset)
    ToggleRow("Dark mode", state.darkMode, modifier = inset) { state.darkMode = it }
    val scaleIndex = FontScales.indexOfFirst { it >= state.fontScale }.coerceAtLeast(0)
    SliderRow(
      label = "Font scale",
      value = formatScale(state.fontScale),
      current = scaleIndex.toFloat(),
      range = 0f..FontScales.lastIndex.toFloat(),
      steps = FontScales.size - 2,
      modifier = inset,
    ) { state.fontScale = FontScales[it.roundToInt().coerceIn(FontScales.indices)] }
  }
}

/** Selection is derived from the current device, so the row stays correct when the device changes
 * from anywhere else. */
@Composable
private fun CategoryRow(state: DioramaState) {
  val current = state.device.category
  val categories = DeviceCategory.entries.filter { category ->
    category == DeviceCategory.Custom || state.devices.any { it.category == category }
  }

  Row(
    // padding after horizontalScroll, so the inset scrolls with the content instead of clipping it
    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = EdgeInset),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    categories.forEach { category ->
      Chip(label = category.name, selected = category == current) {
        if (category == DeviceCategory.Custom) {
          state.selectCustomDevice()
        } else {
          state.devices.firstOrNull { it.category == category }?.let(state::selectDevice)
        }
      }
    }
  }
}

@Composable
private fun DeviceRow(state: DioramaState) {
  if (state.isCustomSelected) {
    CustomDeviceEditor(state, Modifier.padding(horizontal = EdgeInset))
    return
  }

  val group = state.devices.filter { it.category == state.device.category }
  Row(
    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = EdgeInset),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    group.forEach { spec ->
      Chip(label = spec.name, selected = spec.id == state.device.id) { state.selectDevice(spec) }
    }
  }
}

/** Both coordinate spaces at once: a wrong dpi still looks plausible in dp alone. */
@Composable
private fun SpecReadout(state: DioramaState, modifier: Modifier = Modifier) {
  val spec = state.device
  val size = spec.sizeFor(state.orientation)
  val widthDp = size.width.value.roundToInt()
  val heightDp = size.height.value.roundToInt()
  val widthPx = (size.width.value * spec.density).roundToInt()
  val heightPx = (size.height.value * spec.density).roundToInt()

  Row(
    modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Raised)
      .padding(horizontal = 12.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Metric("$widthDp x $heightDp", "dp")
    Metric("$widthPx x $heightPx", "px")
    Metric("${spec.dpi}", "dpi")
  }
}

@Composable
private fun Metric(value: String, unit: String) {
  Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(
      value,
      fontFamily = FontFamily.Monospace,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium,
      color = Signal,
    )
    Text(unit, fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = Muted)
  }
}

@Composable
private fun CustomDeviceEditor(state: DioramaState, modifier: Modifier = Modifier) {
  val size = state.customDevice.screenSize

  Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
    SliderRow(
      label = "Width",
      value = "${size.width.value.roundToInt()} dp",
      current = size.width.value,
      range = CustomDevice.WidthRange.start.value..CustomDevice.WidthRange.endInclusive.value,
    ) { state.updateCustomDevice(width = it.roundToInt().dp) }

    SliderRow(
      label = "Height",
      value = "${size.height.value.roundToInt()} dp",
      current = size.height.value,
      range = CustomDevice.HeightRange.start.value..CustomDevice.HeightRange.endInclusive.value,
    ) { state.updateCustomDevice(height = it.roundToInt().dp) }

    val densities = CustomDevice.Densities
    val index = densities.indexOf(state.customDevice.dpi).coerceAtLeast(0)
    SliderRow(
      label = "Density",
      value = "${state.customDevice.dpi} dpi",
      current = index.toFloat(),
      range = 0f..(densities.lastIndex).toFloat(),
      steps = densities.size - 2,
    ) { state.updateCustomDevice(dpi = densities[it.roundToInt().coerceIn(densities.indices)]) }
  }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
  Text(
    label,
    style = MaterialTheme.typography.labelLarge,
    color = if (selected) Signal else Ink,
    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .background(if (selected) Signal.copy(alpha = 0.16f) else Raised)
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp),
  )
}

@Composable
private fun Divider(modifier: Modifier = Modifier) = HorizontalDivider(modifier, color = Line)

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) {
  Text(
    text.uppercase(),
    style = MaterialTheme.typography.labelSmall,
    color = Muted,
    letterSpacing = 1.sp,
    modifier = modifier,
  )
}

@Composable
private fun SliderRow(
  label: String,
  value: String,
  current: Float,
  range: ClosedFloatingPointRange<Float>,
  steps: Int = 0,
  modifier: Modifier = Modifier,
  onChange: (Float) -> Unit,
) {
  Column(modifier) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
      Text(label, style = MaterialTheme.typography.bodySmall, color = Ink)
      Text(value, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Signal)
    }
    Slider(
      value = current,
      onValueChange = onChange,
      valueRange = range,
      steps = steps,
      colors = SliderDefaults.colors(
        thumbColor = Signal,
        activeTrackColor = Signal,
        inactiveTrackColor = TrackIdle,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
      ),
    )
  }
}

@Composable
private fun ToggleRow(
  label: String,
  checked: Boolean,
  enabled: Boolean = true,
  modifier: Modifier = Modifier,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.bodyMedium,
      color = if (enabled) Ink else Ink.copy(alpha = 0.3f),
    )
    Switch(
      checked = checked,
      onCheckedChange = onCheckedChange,
      enabled = enabled,
      // disabled slots otherwise resolve from the host app's light MaterialTheme
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = Signal,
        uncheckedTrackColor = Raised,
        uncheckedBorderColor = Line,
        disabledCheckedThumbColor = Ink.copy(alpha = 0.4f),
        disabledCheckedTrackColor = Signal.copy(alpha = 0.3f),
        disabledUncheckedThumbColor = Muted.copy(alpha = 0.5f),
        disabledUncheckedTrackColor = Raised,
        disabledUncheckedBorderColor = Line,
      ),
    )
  }
}

private fun formatScale(value: Float): String {
  val rounded = (value * 100).toInt()
  return "${rounded / 100}.${(rounded % 100).toString().padStart(2, '0')}x"
}
