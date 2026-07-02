package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.photobox.PhotoboxViewModel
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCameraManager
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxPendingUploadStore
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadQueue
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module for photobox-related dependencies.
 */
val photoboxModule = module {
    single { PhotoboxCameraManager(androidContext()) }
    single {
        PhotoboxUploadRepository(
            context = androidContext(),
            client = get(),
            photoboxConfigRepository = get()
        )
    }
    single { PhotoboxPendingUploadStore(androidContext()) }
    single { PhotoboxUploadQueue(context = androidContext(), pendingUploadStore = get()) }
    viewModel {
        PhotoboxViewModel(
            cameraManager = get(),
            photoboxConfigRepository = get(),
            uploadRepository = get(),
            uploadQueue = get()
        )
    }
}
