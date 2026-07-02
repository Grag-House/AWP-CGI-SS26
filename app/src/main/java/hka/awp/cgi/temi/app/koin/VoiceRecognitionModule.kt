package hka.awp.cgi.temi.app.koin

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import hka.awp.cgi.temi.app.feature.voiceRecognition.TemiVoiceListener
import hka.awp.cgi.temi.app.feature.voiceRecognition.TemiVoiceManager
import hka.awp.cgi.temi.app.feature.voiceRecognition.TemiVoiceRecognitionViewModel
import hka.awp.cgi.temi.app.feature.voiceRecognition.VoiceProfileRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val voiceRecognitionModule = module {
    single { androidContext().voiceDataStore }
    single { VoiceProfileRepository(get()) }
    single { TemiVoiceManager(androidContext()) }
    single {
        TemiVoiceListener(
            voiceManager = get(),
            robot = get(),
            voiceProfileRepository = get(),
            appConfigRepository = get()
        )
    }
    viewModel {
        TemiVoiceRecognitionViewModel(
            voiceManager = get(),
            temiVoiceListener = get(),
            appConfigRepository = get()
        )
    }
}

private val android.content.Context.voiceDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "voice_settings"
)
