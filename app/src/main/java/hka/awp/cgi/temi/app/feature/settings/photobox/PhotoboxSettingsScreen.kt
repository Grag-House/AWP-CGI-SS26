package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PhotoboxSettingsScreen(
    onBackClick: () -> Unit,
    viewModel: PhotoboxSettingsViewModel = koinViewModel()
) {
    val overlayEnabled by viewModel.overlayEnabled.collectAsState()
    val bannerEnabled by viewModel.bannerSettings.enabled.collectAsState()
    val banner by viewModel.bannerSettings.banner.collectAsState()
    val driveFolderLink by viewModel.driveFolderLink.collectAsState()
    val driveUploadUrl by viewModel.driveUploadUrl.collectAsState()

    PhotoboxSettingsContent(
        onBackClick = onBackClick,
        overlayEnabled = overlayEnabled,
        onOverlayEnabledChange = viewModel::setOverlayEnabled,
        bannerEnabled = bannerEnabled,
        onBannerEnabledChange = viewModel.bannerSettings::setEnabled,
        banner = banner,
        onBannerSelect = viewModel.bannerSettings::setBanner,
        driveFolderLink = driveFolderLink,
        driveUploadUrl = driveUploadUrl,
        onSaveUploadSettings = { folderLink, uploadUrl ->
            viewModel.setDriveFolderLink(folderLink)
            viewModel.setDriveUploadUrl(uploadUrl)
        }
    )
}
