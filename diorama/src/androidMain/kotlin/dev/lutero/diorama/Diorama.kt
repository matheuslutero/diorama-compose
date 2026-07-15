package dev.lutero.diorama

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.lutero.diorama.frame.DeviceFrame
import dev.lutero.diorama.frame.DeviceSpec
import dev.lutero.diorama.frame.DeviceViewport
import dev.lutero.diorama.frame.Devices

private val BezelWidth = 12.dp

/** Light enough to read the near-black bezel against. */
internal val StageBackground = Color(0xFFCECED6)
internal val PanelBackground = Color(0xFF16161C)

/**
 * Wraps an app so it renders inside a simulated device, with a bar to toggle the simulation and
 * open the settings drawer.
 *
 * Unlike Flutter's device_preview this takes the content directly rather than a builder: a
 * @Composable lambda re-executes wherever it is invoked, so it reads whatever CompositionLocals
 * are in scope at the call site with no indirection needed.
 *
 * Pass [devices] to extend or replace the catalog. A DeviceSpec is an ordinary data class, so a
 * project can add its own hardware without this library knowing about it:
 *
 * ```
 * Diorama(devices = Devices.All + DeviceSpec("kiosk", "Kiosk", DpSize(1024.dp, 600.dp), dpi = 160))
 * ```
 *
 * The panel also carries one runtime-editable device, so a size can be dialled in without a
 * rebuild. See [DioramaState.customDevice].
 */
@Composable
fun Diorama(
  modifier: Modifier = Modifier,
  devices: List<DeviceSpec> = Devices.All,
  state: DioramaState = rememberDioramaState(devices),
  content: @Composable () -> Unit,
) {
  val currentContent by rememberUpdatedState(content)

  // movableContentOf keeps the app's composition (and so its state) alive across the simulated and
  // unsimulated branches; without it every toggle remounts the app from scratch.
  val app = remember { movableContentOf { currentContent() } }

  BoxWithConstraints(modifier.fillMaxSize().background(StageBackground)) {
    val drawerMaxHeight = maxHeight * 0.5f

    Column(Modifier.fillMaxSize()) {
      Box(Modifier.weight(1f).fillMaxWidth()) {
        if (state.isEnabled) Stage(state, app) else app()
      }

      Surface(color = PanelBackground, contentColor = Color.White) {
        Column(Modifier.navigationBarsPadding()) {
          DioramaBar(state)
          AnimatedVisibility(
            visible = state.isPanelOpen,
            enter = expandVertically(),
            exit = shrinkVertically(),
          ) {
            DioramaPanel(state, Modifier.heightIn(max = drawerMaxHeight))
          }
        }
      }
    }
  }
}

@Composable
private fun Stage(state: DioramaState, app: @Composable () -> Unit) {
  // no navigationBarsPadding here: the dock below owns it
  Box(
    Modifier.fillMaxSize().statusBarsPadding().displayCutoutPadding().padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    DeviceOverride(
      spec = state.device,
      orientation = state.orientation,
      fontScale = state.fontScale,
      darkMode = state.darkMode,
    ) {
      val screen = state.device.sizeFor(state.orientation)
      val bezel = if (state.isFrameVisible) BezelWidth else 0.dp

      // bezel and screen scale as one unit; DeviceFrame pads the bezel back off
      DeviceViewport(DpSize(screen.width + bezel * 2, screen.height + bezel * 2)) {
        DeviceFrame(bezel) { app() }
      }
    }
  }
}
