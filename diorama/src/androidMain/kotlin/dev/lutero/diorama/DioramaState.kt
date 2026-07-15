package dev.lutero.diorama

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.lutero.diorama.frame.DeviceSpec
import dev.lutero.diorama.frame.Devices
import dev.lutero.diorama.frame.Orientation

@Stable
class DioramaState(initialDevice: DeviceSpec) {
  var isEnabled by mutableStateOf(true)
  var isPanelOpen by mutableStateOf(false)
  var device by mutableStateOf(initialDevice)
  var orientation by mutableStateOf(Orientation.Portrait)
  var darkMode by mutableStateOf(false)
  var isFrameVisible by mutableStateOf(true)

  /**
   * Defaults to 1f rather than the host's value on purpose. Configuration.updateFrom merges onto
   * the host config, so any axis left unset inherits it — a device with a 2x accessibility font
   * scale would otherwise render every simulated device at 2x.
   */
  var fontScale by mutableFloatStateOf(1f)

  fun rotate() {
    if (!device.canRotate) return
    orientation =
      if (orientation == Orientation.Portrait) Orientation.Landscape else Orientation.Portrait
  }
}

/**
 * Survives Activity recreation, which rotating the host triggers — losing the simulated device on
 * every rotation defeats the tool.
 *
 * The device is stored by id rather than by value: DeviceSpec is not Parcelable, and the id is the
 * stable identifier anyway. [initialDevice] joins the lookup so a caller-supplied device outside
 * [Devices.All] still restores.
 */
private fun dioramaStateSaver(initialDevice: DeviceSpec): Saver<DioramaState, Any> {
  val byId = (Devices.All + initialDevice).associateBy { it.id }
  return listSaver(
    save = { state ->
      listOf(
        state.isEnabled,
        state.isPanelOpen,
        state.device.id,
        state.orientation.name,
        state.darkMode,
        state.isFrameVisible,
        state.fontScale,
      )
    },
    restore = { saved ->
      DioramaState(byId[saved[2] as String] ?: initialDevice).apply {
        isEnabled = saved[0] as Boolean
        isPanelOpen = saved[1] as Boolean
        orientation = Orientation.valueOf(saved[3] as String)
        darkMode = saved[4] as Boolean
        isFrameVisible = saved[5] as Boolean
        fontScale = saved[6] as Float
      }
    },
  )
}

// TODO(persistence): DataStore, written from snapshotFlow { ... }.debounce(500), to carry settings
//   across process death and relaunches. The saver below only covers configuration changes.
@Composable
fun rememberDioramaState(initialDevice: DeviceSpec = Devices.Phone): DioramaState =
  rememberSaveable(initialDevice, saver = dioramaStateSaver(initialDevice)) {
    DioramaState(initialDevice)
  }
