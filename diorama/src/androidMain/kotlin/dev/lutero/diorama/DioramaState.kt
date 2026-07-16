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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.lutero.diorama.frame.CustomDevice
import dev.lutero.diorama.frame.DeviceSpec
import dev.lutero.diorama.frame.Devices
import dev.lutero.diorama.frame.Orientation

@Stable
class DioramaState(
  /** The picker's catalog. Pass your own to add devices or replace the defaults entirely. */
  val devices: List<DeviceSpec>,
  initialDevice: DeviceSpec,
) {
  var isEnabled by mutableStateOf(true)
  var isPanelOpen by mutableStateOf(false)
  var device by mutableStateOf(initialDevice)
    private set
  var orientation by mutableStateOf(Orientation.Portrait)
    private set
  var darkMode by mutableStateOf(false)
  var isFrameVisible by mutableStateOf(true)

  /**
   * Defaults to 1f on purpose: Configuration.updateFrom merges onto the host config, so an unset
   * axis inherits the host's accessibility font scale.
   */
  var fontScale by mutableFloatStateOf(1f)

  /** Editable at runtime. Kept even while a catalog device is selected, so edits are not lost. */
  var customDevice by mutableStateOf(CustomDevice.Default)
    private set

  val isCustomSelected: Boolean get() = device.id == CustomDevice.Id

  fun selectDevice(spec: DeviceSpec) {
    device = spec
    if (!spec.canRotate) orientation = Orientation.Portrait
  }

  fun selectCustomDevice() = selectDevice(customDevice)

  fun updateCustomDevice(
    width: Dp = customDevice.screenSize.width,
    height: Dp = customDevice.screenSize.height,
    dpi: Int = customDevice.dpi,
  ) {
    customDevice = CustomDevice.of(width, height, dpi)
    if (isCustomSelected) device = customDevice
  }

  fun rotate() {
    if (!device.canRotate) return
    orientation =
      if (orientation == Orientation.Portrait) Orientation.Landscape else Orientation.Portrait
  }
}

/**
 * Catalog devices are stored by id rather than by value, since DeviceSpec is not Parcelable. The
 * custom device has no id to look up, so its three editable fields are stored instead.
 */
private fun dioramaStateSaver(
  devices: List<DeviceSpec>,
  initialDevice: DeviceSpec,
): Saver<DioramaState, Any> {
  val byId = (devices + initialDevice).associateBy { it.id }
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
        state.customDevice.screenSize.width.value,
        state.customDevice.screenSize.height.value,
        state.customDevice.dpi,
      )
    },
    // Restoring through the public API cannot produce a state the API would refuse, e.g. landscape
    // on a device that cannot rotate.
    restore = { saved ->
      DioramaState(devices, initialDevice).apply {
        updateCustomDevice(
          width = (saved[7] as Float).dp,
          height = (saved[8] as Float).dp,
          dpi = saved[9] as Int,
        )
        val savedId = saved[2] as String
        selectDevice(
          if (savedId == CustomDevice.Id) customDevice else byId[savedId] ?: initialDevice,
        )
        if (Orientation.valueOf(saved[3] as String) == Orientation.Landscape) rotate()
        isEnabled = saved[0] as Boolean
        isPanelOpen = saved[1] as Boolean
        darkMode = saved[4] as Boolean
        isFrameVisible = saved[5] as Boolean
        fontScale = saved[6] as Float
      }
    },
  )
}

// TODO(persistence): DataStore, written from snapshotFlow { ... }.debounce(500), to carry settings
//   across process death and relaunches. The saver above only covers configuration changes.
@Composable
fun rememberDioramaState(
  devices: List<DeviceSpec> = Devices.All,
  initialDevice: DeviceSpec = devices.first(),
): DioramaState =
  rememberSaveable(devices, initialDevice, saver = dioramaStateSaver(devices, initialDevice)) {
    DioramaState(devices, initialDevice)
  }
