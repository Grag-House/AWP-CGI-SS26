package hka.awp.cgi.temi.app.feature.weatherscreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R

// ─── Data models ─────────────────────────────────────────────────────────────

enum class WeatherIcon { SUN, CLOUD, SUN_CLOUD, RAIN, SNOW, THUNDER, FOG }

private val hourlyData = WeatherCards.setHourlyWeatherCards()

private val dailyData = WeatherCards.setDailyWeatherCards()

// ─── Weather icon helper ──────────────────────────────────────────────────────

@Composable
fun WeatherIconView(icon: WeatherIcon, size: Int = 28) {
    val sizeDp = size.dp
    when (icon) {
        WeatherIcon.SUN -> Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = stringResource(R.string.icon_sun),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )

        WeatherIcon.CLOUD -> Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = stringResource(R.string.icon_cloudy),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )

        WeatherIcon.SUN_CLOUD -> Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.WbCloudy, // TODO: besseres Icon finden, im Moment nur eine Wolke ohne Sonne
                contentDescription = stringResource(R.string.icon_partlycloudy),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(sizeDp)
            )
        }

        WeatherIcon.RAIN -> Icon(
            imageVector = Icons.Default.Umbrella, // TODO: replace icon with raincloud
            contentDescription = stringResource(R.string.icon_rain),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )

        WeatherIcon.SNOW -> Icon(
            imageVector = Icons.Default.AcUnit,
            contentDescription = stringResource(R.string.icon_snow),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )

        WeatherIcon.THUNDER -> Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = stringResource(R.string.icon_thunder),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )

        WeatherIcon.FOG -> Icon(
            imageVector = Icons.Default.BlurOn, // TODO: nach passenderem Icon schauen
            contentDescription = stringResource(R.string.icon_fog),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(sizeDp)
        )
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
                    stringResource(R.string.location_kalrsruhe),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    hourlyData[0].temp + stringResource(R.string.temp_celsius),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            WeatherIconView(hourlyData[0].icon, 80)
        }
    }
}

@Composable
fun HourlyForecastCard() {
    WeatherCard {
        Text(
            stringResource(R.string.daily_outlook),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hourlyData[0].label = stringResource(R.string.now)
            hourlyData.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(item.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    WeatherIconView(item.icon, size = 20)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.temp + stringResource(R.string.degree),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.precipitation + stringResource(R.string.percent),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyForecastCard() {
    WeatherCard {
        Text(
            stringResource(R.string.weekly_outlook),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            dailyData[0].day = stringResource(R.string.today)
            dailyData.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(item.day, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    Spacer(Modifier.height(4.dp))
                    WeatherIconView(item.icon, size = 22)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.high + stringResource(R.string.degree),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        item.low + stringResource(R.string.degree),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
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
                Icon(
                    Icons.Default.Cloud,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.weather),
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
        actions = {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 16.dp)
            )
        }
    )
}

// ─── Main screen ─────────────────────────────────────────────────────────────

@Composable
fun WeatherContent() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(color = 0xFFF5F5F5))
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
