package io.github.matheuslutero.diorama.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import io.github.matheuslutero.diorama.Diorama

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

private data class Place(
  val name: String,
  val region: String,
  val tag: String,
  val blurb: String,
  val timeZone: String,
  val season: String,
  val colors: Pair<Color, Color>,
)

private val Places = listOf(
  Place(
    "Santorini", "Greece", "Island",
    "Whitewashed villages on the rim of a drowned volcano. The Oia sunset empties every terrace in town.",
    "GMT+3", "Apr – Oct", Color(0xFF2193B0) to Color(0xFF0B2E6B),
  ),
  Place(
    "Kyoto", "Japan", "City",
    "Two thousand temples, one bamboo grove, and streets where geiko still hurry to work at dusk.",
    "GMT+9", "Mar – May", Color(0xFFEE9CA7) to Color(0xFF8E2445),
  ),
  Place(
    "Machu Picchu", "Peru", "Trail",
    "The Inca citadel appears through the Sun Gate at dawn if you walk the last stretch of the trail.",
    "GMT-5", "May – Sep", Color(0xFF11998E) to Color(0xFF2C3E50),
  ),
  Place(
    "Bora Bora", "French Polynesia", "Beach",
    "A lagoon three shades of blue around one green peak. Overwater bungalows invented themselves here.",
    "GMT-10", "May – Oct", Color(0xFF43C6AC) to Color(0xFF191654),
  ),
  Place(
    "Banff", "Canada", "Trail",
    "Glacier lakes the color of toothpaste and elk on the main street. June trails still cross snow.",
    "GMT-7", "Jun – Sep", Color(0xFF56CCF2) to Color(0xFF2F80ED),
  ),
  Place(
    "Amalfi Coast", "Italy", "Beach",
    "Lemon groves and pastel towns bolted to sea cliffs, linked by a road with no straight meters.",
    "GMT+1", "May – Sep", Color(0xFFFF512F) to Color(0xFFDD2476),
  ),
)

private val Filters = listOf("All", "Beach", "City", "Island", "Trail")

@Composable
private fun SampleApp() {
  val dark = isSystemInDarkTheme()
  MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
      val twoPane = currentWindowAdaptiveInfo().windowSizeClass
        .isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)
      var selected by remember { mutableStateOf(Places.first()) }

      if (twoPane) {
        Row(Modifier.fillMaxSize().safeDrawingPadding()) {
          Catalog(
            selected = selected,
            onSelect = { selected = it },
            modifier = Modifier.weight(1f),
          )
          DetailPane(
            place = selected,
            modifier = Modifier.width(340.dp).fillMaxSize(),
          )
        }
      } else {
        Catalog(
          selected = selected,
          onSelect = { selected = it },
          modifier = Modifier.fillMaxSize().safeDrawingPadding(),
        )
      }
    }
  }
}

@Composable
private fun Catalog(
  selected: Place,
  onSelect: (Place) -> Unit,
  modifier: Modifier = Modifier,
) {
  var filter by remember { mutableStateOf("All") }
  val shown = if (filter == "All") Places else Places.filter { it.tag == filter }

  Column(modifier.padding(horizontal = 20.dp)) {
    Spacer(Modifier.height(24.dp))
    Text("Atlas", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    Text(
      "${Places.size} places worth the detour",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    )

    Spacer(Modifier.height(16.dp))
    Row(
      Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Filters.forEach { name ->
        FilterChip(name, selected = name == filter) { filter = name }
      }
    }

    Spacer(Modifier.height(16.dp))
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 168.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      items(shown, key = { it.name }) { place ->
        PlaceCard(place, isSelected = place == selected) { onSelect(place) }
      }
    }
  }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
  val colors = MaterialTheme.colorScheme
  Text(
    label,
    style = MaterialTheme.typography.labelLarge,
    color = if (selected) colors.onPrimary else colors.onSurfaceVariant,
    modifier = Modifier
      .clip(RoundedCornerShape(50))
      .background(if (selected) colors.primary else colors.surfaceVariant)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp),
  )
}

@Composable
private fun PlaceCard(place: Place, isSelected: Boolean, onClick: () -> Unit) {
  Card(
    onClick = onClick,
    colors = CardDefaults.cardColors(
      containerColor =
        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
    ),
  ) {
    Box(
      Modifier
        .fillMaxWidth()
        .height(96.dp)
        .background(Brush.linearGradient(listOf(place.colors.first, place.colors.second))),
    )
    Column(Modifier.padding(12.dp)) {
      Text(
        place.name,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
      )
      Text(
        place.region,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
      )
      Spacer(Modifier.height(6.dp))
      Tag(place.tag)
    }
  }
}

@Composable
private fun Tag(text: String) {
  Text(
    text,
    style = MaterialTheme.typography.labelSmall,
    color = MaterialTheme.colorScheme.primary,
    modifier = Modifier
      .clip(RoundedCornerShape(4.dp))
      .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
      .padding(horizontal = 6.dp, vertical = 2.dp),
  )
}

@Composable
private fun DetailPane(place: Place, modifier: Modifier = Modifier) {
  Surface(modifier, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)) {
    Column(Modifier.padding(20.dp)) {
      Spacer(Modifier.height(4.dp))
      Box(
        Modifier
          .fillMaxWidth()
          .height(160.dp)
          .clip(RoundedCornerShape(16.dp))
          .background(Brush.linearGradient(listOf(place.colors.first, place.colors.second))),
      )
      Spacer(Modifier.height(16.dp))
      Text(place.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
      Text(
        place.region,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(Modifier.height(12.dp))
      Text(place.blurb, style = MaterialTheme.typography.bodyMedium)
      Spacer(Modifier.height(20.dp))
      Stat("Time zone", place.timeZone)
      Stat("Best season", place.season)
      Stat("Category", place.tag)
    }
  }
}

@Composable
private fun Stat(label: String, value: String) {
  Row(
    Modifier.fillMaxWidth().padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
    Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
  }
}
