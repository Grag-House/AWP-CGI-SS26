package hka.awp.cgi.temi.app.feature.settings.display

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DisplayScreen(
    onBackClick: () -> Unit,
    viewModel: DisplayViewModel = koinViewModel()
                 ) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    DisplayContent(
        onBackClick = onBackClick,
        isDarkMode = isDarkMode,
        onDarkModeChange = viewModel::toggleDarkMode,
                  )
}
