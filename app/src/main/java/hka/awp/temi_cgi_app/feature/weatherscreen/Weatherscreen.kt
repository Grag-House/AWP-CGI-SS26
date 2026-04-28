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

// ─── Colors ──────────────────────────────────────────────────────────────────
val CGIRed      = Color(0xFFCC0033)
val SidebarBg   = Color(0xFFF0F0F0)
val CardBg      = Color.White
val TextPrimary = Color(0xFF1A1A1A)
val TextMuted   = Color(0xFF888888)
val ActiveItem  = Color(0xFFDDDDDD)

// ─── Data models ─────────────────────────────────────────────────────────────

enum class WeatherIcon { SUN, CLOUD, SUN_CLOUD, RAIN }

data class HourlyItem(
    val label: String,       // "Jetzt", "t+1", …
    val icon: WeatherIcon,
    val temp: String,        // "21°"
    val rain: String         // "0%"
)

data class DailyItem(
    val day: String,         // "Heute", "Do", …
    val icon: WeatherIcon,
    val high: String,
    val low: String
)

// ─── Sample data ─────────────────────────────────────────────────────────────

private val hourlyData = listOf(
    HourlyItem("Jetzt", WeatherIcon.SUN,       "21°", "0%"),
    HourlyItem("t+1",  WeatherIcon.RAIN,       "21°", "80%"),
    HourlyItem("t+2",  WeatherIcon.CLOUD,      "21°", "10%"),
    HourlyItem("t+3",  WeatherIcon.SUN_CLOUD,  "21°", "0%"),
    HourlyItem("t+4",  WeatherIcon.RAIN,       "21°", "75%"),
    HourlyItem("t+5",  WeatherIcon.RAIN,       "21°", "90%"),
    HourlyItem("t+6",  WeatherIcon.RAIN,       "21°", "82%"),
    HourlyItem("t+7",  WeatherIcon.CLOUD,      "5°",  "75%"),
    HourlyItem("t+8",  WeatherIcon.RAIN,       "21°", "64%"),
    HourlyItem("t+9",  WeatherIcon.RAIN,       "21°", "66%"),
)

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
            tint = CGIRed,
            modifier = Modifier.size(sizeDp)
        )
        WeatherIcon.CLOUD -> Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = "Cloudy",
            tint = CGIRed,
            modifier = Modifier.size(sizeDp)
        )
        WeatherIcon.SUN_CLOUD -> Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.WbCloudy,
                contentDescription = "Partly cloudy",
                tint = CGIRed,
                modifier = Modifier.size(sizeDp)
            )
        }
        WeatherIcon.RAIN -> Icon(
            imageVector = Icons.Default.Umbrella,
            contentDescription = "Rain",
            tint = CGIRed,
            modifier = Modifier.size(sizeDp)
        )
    }
}

// ─── Sidebar ──────────────────────────────────────────────────────────────────

data class SidebarEntry(val label: String, val icon: ImageVector, val active: Boolean = false)

@Composable
fun Sidebar(modifier: Modifier = Modifier) {
    val entries = listOf(
        SidebarEntry("Hauptmenü",   Icons.Default.Home,        active = true),
        SidebarEntry("Webserver",   Icons.Default.Storage),
        SidebarEntry("Wetter",      Icons.Default.Cloud,       active = false),
        SidebarEntry("Navigation",  Icons.Default.Navigation),
        SidebarEntry("Modus",       Icons.Default.ToggleOn),
        SidebarEntry("Einstellungen", Icons.Default.Settings),
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(SidebarBg)
            .padding(horizontal = 8.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Brand
            Text(
                text = "CGI",
                color = CGIRed,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
            Text(
                text = "Funktionen",
                color = CGIRed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )

            entries.forEach { entry ->
                val bg   = if (entry.label == "Hauptmenü") CGIRed
                else if (entry.label == "Wetter") ActiveItem
                else Color.Transparent
                val fg   = if (entry.label == "Hauptmenü") Color.White else TextPrimary

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .background(bg, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = entry.label,
                        tint = fg,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(entry.label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Help button at bottom
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(ActiveItem, RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Icon(Icons.Default.Help, contentDescription = "Hilfe", tint = TextMuted, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Hilfe", color = TextMuted, fontSize = 13.sp)
        }
    }
}

// ─── Cards ────────────────────────────────────────────────────────────────────

@Composable
fun WeatherCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                    color = TextPrimary
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "21°C",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = CGIRed
                )
            }
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = "Sunny",
                tint = CGIRed,
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
            color = TextPrimary
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
                    Text(item.label,    color = TextMuted,   fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    WeatherIconView(item.icon, size = 20)
                    Spacer(Modifier.height(4.dp))
                    Text(item.temp,     color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(item.rain,     color = TextMuted,   fontSize = 10.sp)
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
            color = TextPrimary
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
                    Text(item.day,  color = TextMuted,   fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    WeatherIconView(item.icon, size = 22)
                    Spacer(Modifier.height(4.dp))
                    Text(item.high, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(item.low,  color = TextMuted,   fontSize = 10.sp)
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
                Icon(Icons.Default.Cloud, contentDescription = null, tint = CGIRed, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(8.dp))
                Text("Wetter", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = TextPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
        actions = {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = TextPrimary, modifier = Modifier.padding(end = 16.dp))
        }
    )
}

// ─── Main screen ─────────────────────────────────────────────────────────────

@Composable
fun WetterScreen() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Sidebar
        Sidebar(modifier = Modifier.width(180.dp))

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
fun WetterScreenPreview() {
    MaterialTheme {
        WetterScreen()
    }
}