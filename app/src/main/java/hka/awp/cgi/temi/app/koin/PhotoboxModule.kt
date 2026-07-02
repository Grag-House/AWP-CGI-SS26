package hka.awp.cgi.temi.app.koin

import hka.awp.cgi.temi.app.feature.photobox.PhotoboxViewModel
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCameraManager
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxPendingUploadStore
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadQueue
import hka.awp.cgi.temi.app.feature.photobox.upload.PhotoboxUploadRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val photoboxModule = module {
    single { PhotoboxCameraManager(androidContext()) }
    single {
        PhotoboxUploadRepository(
            context = androidContext(),
            client = get(),
            appConfigRepository = get()
        )
    }
    single { PhotoboxPendingUploadStore(androidContext()) }
    single { PhotoboxUploadQueue(context = androidContext(), pendingUploadStore = get()) }
    viewModel {
        PhotoboxViewModel(
            cameraManager = get(),
            appConfigRepository = get(),
            uploadRepository = get(),
            uploadQueue = get()
        )
    }
}
