package hka.awp.temi_cgi_app.feature.weatherscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Data models ─────────────────────────────────────────────────────────────

enum class WeatherIcon { SUN, CLOUD, SUN_CLOUD, RAIN, SNOW, THUNDER, FOG }

private val hourlyData = WeatherCards.setHourlyWeatherCards()

private val dailyData = listOf(
    DailyItem("Heute", WeatherIcon.SUN, "21°", "3°"),
    DailyItem("Do",    WeatherIcon.SUN, "21°", "3°"),
    DailyItem("Fr",    WeatherIcon.SUN, "21°", "3°"),
    DailyItem("Sa",    WeatherIcon.SUN, "21°", "3°"),
    DailyItem("So",    WeatherIcon.SUN, "21°", "3°"),
    DailyItem("Mo",    WeatherIcon.SUN, "21°", "3°"),
    DailyItem("Di",    WeatherIcon.SUN, "21°", "3°"),
)

// ─── Weather icon helper ──────────────────────────────────────────────────────

@Composable
fun WeatherIconView(icon: WeatherIcon, size: Int = 28) {
    val sizeDp = size.dp
    when (icon) {
        WeatherIcon.SUN -> Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = "Sunny",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )
        WeatherIcon.CLOUD -> Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = "Cloudy",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )
        WeatherIcon.SUN_CLOUD -> Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.WbCloudy,
                contentDescription = "Partly cloudy",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(sizeDp)
            )
        }
        WeatherIcon.RAIN -> Icon(
            imageVector = Icons.Default.Umbrella, //TODO: replace icon with raincloud
            contentDescription = "Rain",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )
        WeatherIcon.SNOW -> TODO()
        WeatherIcon.THUNDER -> TODO()
        WeatherIcon.FOG -> TODO()
    }
}
// ─── Cards ────────────────────────────────────────────────────────────────────

@Composable
fun WeatherCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun CurrentWeatherCard() {
    WeatherCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Standort: Karlsruhe",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "21°C",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = "Sunny",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )
        }
    }

}

@Composable
fun HourlyForecastCard() {
    WeatherCard {
        Text(
            "Täglicher Ausblick",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hourlyData.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(item.label,    color = MaterialTheme.colorScheme.onSurfaceVariant,   fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    WeatherIconView(item.icon, size = 20)
                    Spacer(Modifier.height(4.dp))
                    Text(item.temp,     color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(item.precipitation,     color = MaterialTheme.colorScheme.onSurfaceVariant,   fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun WeeklyForecastCard() {
    WeatherCard {
        Text(
            "Nächste Woche",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyData.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(item.day,  color = MaterialTheme.colorScheme.onSurfaceVariant,   fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    WeatherIconView(item.icon, size = 22)
                    Spacer(Modifier.height(4.dp))
                    Text(item.high, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(item.low,  color = MaterialTheme.colorScheme.onSurfaceVariant,   fontSize = 10.sp)
                }
            }
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WetterTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Wetter", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
        actions = {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(end = 16.dp))
        }
    )
}

// ─── Main screen ─────────────────────────────────────────────────────────────

@Composable
fun WeatherContent() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Main content
        Column(modifier = Modifier.weight(1f)) {
            WetterTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CurrentWeatherCard()
                HourlyForecastCard()
                WeeklyForecastCard()
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, widthDp = 800, heightDp = 600)
@Composable
fun WeatherContentPreview() {
    MaterialTheme {
        WeatherContent()
    }
}