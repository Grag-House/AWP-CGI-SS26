package hka.awp.cgi.temi.app.feature.settings.photobox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

/**
 * Stateful entry point screen for the Photobox settings feature.
 *
 * This composable connects presentation nodes with back-end architectures. It initializes
 * the [PhotoboxSettingsViewModel] via Koin dependency injection, observes standard and nested state
 * data structures as reactive Compose elements, and binds UI interactions back to their corresponding
 * business logic mutation endpoints.
 *
 * @param onBackClick Executed when the user targets a navigation exit action out of this screen layer.
 * @param viewModel The state management and persistence coordinator for photobox values, injected via Koin by default.
 */
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
