package dev.lutero.diorama.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import dev.lutero.diorama.Diorama

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      Diorama {
        SampleApp()
      }
    }
  }
}

/** Reports the values the simulation drives; every line should move on a device switch. */
@Composable
private fun SampleApp() {
  val dark = isSystemInDarkTheme()
  MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val sizeClass = currentWindowAdaptiveInfo().windowSizeClass

    Column(
      Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Text("Diorama", style = MaterialTheme.typography.headlineSmall)
      Reading("window size class", "$sizeClass")
      Reading("containerSize", "${LocalWindowInfo.current.containerSize}")
      Reading("screen dp", "${configuration.screenWidthDp} x ${configuration.screenHeightDp}")
      Reading("densityDpi", "${configuration.densityDpi}")
      Reading("density", "${density.density}")
      Reading("fontScale", "${density.fontScale}")
      Reading("dark mode", "$dark")
    }
  }
}

@Composable
private fun Reading(label: String, value: String) {
  Column {
    Text(
      label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    )
    Text(value, style = MaterialTheme.typography.bodyMedium)
  }
}
