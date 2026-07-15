package dev.lutero.diorama

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import dev.lutero.diorama.frame.DeviceViewport

private val BezelWidth = 12.dp

/** Light enough to read the near-black bezel against, like a backdrop behind a model. */
private val StageBackground = Color(0xFFCECED6)

/**
 * Wraps an app so it renders inside a simulated device, with a bar to toggle the simulation and
 * open the settings sheet.
 *
 * Unlike Flutter's device_preview this takes the content directly rather than a builder: a
 * @Composable lambda re-executes wherever it is invoked, so it reads whatever CompositionLocals
 * are in scope at the call site with no indirection needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Diorama(
  modifier: Modifier = Modifier,
  state: DioramaState = rememberDioramaState(),
  content: @Composable () -> Unit,
) {
  val currentContent by rememberUpdatedState(content)

  // movableContentOf preserves the app's composition — and so all of its state — as it moves
  // between the simulated and unsimulated branches and across device switches. device_preview needs
  // a GlobalKey for the same reason; without it every toggle remounts the app from scratch.
  val app = remember { movableContentOf { currentContent() } }

  Column(modifier.fillMaxSize().background(StageBackground)) {
    Box(Modifier.weight(1f).fillMaxWidth()) {
      if (state.isEnabled) Stage(state, app) else app()
    }
    DioramaBar(state)
  }

  if (state.isPanelOpen) {
    // The sheet is a sibling of the preview rather than an ancestor, and gets its own window, so
    // the device overrides cannot reach the panel's own UI.
    ModalBottomSheet(
      onDismissRequest = { state.isPanelOpen = false },
      sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
      containerColor = Color(0xFF1B1B20),
      contentColor = Color.White,
    ) {
      DioramaPanel(state)
    }
  }
}

@Composable
private fun Stage(state: DioramaState, app: @Composable () -> Unit) {
  // Keeps the frame clear of the host's status bar and cutout. The bar below handles the
  // navigation bar, so the stage deliberately does not pad for it.
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

      // Measure bezel and screen as one unit so a single scale covers both; DeviceFrame pads the
      // bezel back off, handing the app exactly `screen`.
      DeviceViewport(DpSize(screen.width + bezel * 2, screen.height + bezel * 2)) {
        DeviceFrame(bezel) { app() }
      }
    }
  }
}
