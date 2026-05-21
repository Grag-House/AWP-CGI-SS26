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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.R

/**
 * A Composable that displays a weather icon based on the provided [WeatherIcon] type.
 *
 * This view maps internal weather states to specific Material Design icons, applying
 * a consistent tint and size.
 *
 * @param icon The [WeatherIcon] enum value representing the weather condition to display.
 * @param modifier The [Modifier] to be applied to the icon or its container.
 * @param size The size of the icon in density-independent pixels (dp). Defaults to 28.
 */
@Composable
fun WeatherIconView(icon: WeatherIcon, modifier: Modifier = Modifier, size: Int = 28) {
    val sizeDp = size.dp
    when (icon) {
        WeatherIcon.SUN -> Icon(
            imageVector = Icons.Default.WbSunny,
            contentDescription = stringResource(R.string.icon_sun),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(sizeDp)
                               )

        WeatherIcon.CLOUD -> Icon(
            imageVector = Icons.Default.Cloud,
            contentDescription = stringResource(R.string.icon_cloudy),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(sizeDp)
                                 )

        WeatherIcon.SUN_CLOUD -> Box(contentAlignment = Alignment.Center, modifier = modifier) {
            Icon(
                imageVector = Icons.Default.WbCloudy,
                contentDescription = stringResource(R.string.icon_partlycloudy),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(sizeDp)
                )
        }

        WeatherIcon.RAIN -> Icon(
            imageVector = Icons.Default.Umbrella,
            contentDescription = stringResource(R.string.icon_rain),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(sizeDp)
                                )

        WeatherIcon.SNOW -> Icon(
            imageVector = Icons.Default.AcUnit,
            contentDescription = stringResource(R.string.icon_snow),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(sizeDp)
                                )

        WeatherIcon.THUNDER -> Icon(
            imageVector = Icons.Default.Bolt,
            contentDescription = stringResource(R.string.icon_thunder),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(sizeDp)
                                   )

        WeatherIcon.FOG -> Icon(
            imageVector = Icons.Default.BlurOn,
            contentDescription = stringResource(R.string.icon_fog),
            tint = MaterialTheme.colorScheme.primary,
            modifier = modifier.size(sizeDp)
                               )
    }
}
// ─── Cards ────────────────────────────────────────────────────────────────────

/**
 * A reusable container component that provides a consistent layout and styling for weather-related information.
 * It wraps content in a [Card] with a specific background color, rounded corners, and internal padding.
 *
 * @param modifier The [Modifier] to be applied to the card.
 * @param content The composable content to be displayed inside the card's column layout.
 */
@Composable
fun WeatherCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
        ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

/**
 * Displays a card with current weather information for a given location.
 *
 * @param location The name of the location to display.
 * @param currentHour The [HourlyItem] containing current temperature and weather icon.
 */
@Composable
fun CurrentWeatherCard(location: String, currentHour: HourlyItem?) {
    WeatherCard {
        if (currentHour == null) return@WeatherCard

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
           ) {
            Column {
                Text(
                    location,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                    )
                Spacer(Modifier.height(12.dp))
                Text(
                    currentHour.temp + stringResource(R.string.temp_celsius),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                    )
            }

            WeatherIconView(currentHour.icon, size = 80)
        }
    }
}

/**
 * Displays a card with a horizontal forecast for the upcoming hours.
 *
 * @param hourlyData A list of [HourlyItem] representing the hourly weather forecast.
 */
@Composable
fun HourlyForecastCard(hourlyData: List<HourlyItem>) {
    WeatherCard {
        Text(
            stringResource(R.string.daily_outlook),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
            )
        Spacer(Modifier.height(12.dp))
        if (hourlyData.isEmpty()) return@WeatherCard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
           ) {
            hourlyData.forEachIndexed { index, item ->
                val label = if (index == 0) stringResource(R.string.now) else item.label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                      ) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
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

/**
 * Displays a card with a weekly weather outlook.
 *
 * @param dailyData A list of [DailyItem] representing the daily weather forecast for the week.
 */
@Composable
fun WeeklyForecastCard(dailyData: List<DailyItem>) {
    WeatherCard {
        Text(
            stringResource(R.string.weekly_outlook),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
            )
        Spacer(Modifier.height(12.dp))
        if (dailyData.isEmpty()) return@WeatherCard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
           ) {
            dailyData.forEachIndexed { index, item ->
                val dayLabel = if (index == 0) stringResource(R.string.today) else item.day
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                      ) {
                    Text(dayLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
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

/**
 * Composable for the top app bar of the weather screen.
 */
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
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
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

/**
 * Main content composable for the weather screen.
 *
 * @param viewModel The [WeatherViewModel] that provides the UI state.
 */
@Composable
fun WeatherContent(viewModel: WeatherViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
       ) {
        Column(modifier = Modifier.weight(1f)) {
            WetterTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
                  ) {
                CurrentWeatherCard(
                    location = uiState.location,
                    currentHour = uiState.hourlyForecast.firstOrNull()
                                  )
                HourlyForecastCard(hourlyData = uiState.hourlyForecast)
                WeeklyForecastCard(dailyData = uiState.weeklyForecast)
            }
        }
    }
}
