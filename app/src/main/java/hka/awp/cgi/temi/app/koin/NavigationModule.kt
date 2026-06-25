package hka.awp.cgi.temi.app.koin

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import hka.awp.cgi.temi.app.feature.navigation.NavigationViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import java.util.UUID

/**
 * Koin module for navigation and MQTT-related dependencies.
 */
val navigationModule = module {
    single {
        val client = Mqtt5Client.builder()
            .identifier("temi-android-${UUID.randomUUID()}")
            .serverHost(BuildConfig.MQTT_HOST)
            .serverPort(BuildConfig.MQTT_PORT)
            .simpleAuth(
                Mqtt5SimpleAuth.builder()
                    .username(BuildConfig.MQTT_USERNAME)
                    .password(BuildConfig.MQTT_PASSWORD.toByteArray())
                    .build()
            )
            .buildBlocking()
        return@single client
    }

    single {
        MqttManager(
            robot = get(),
            client = get()
        )
    }

    viewModel {
        NavigationViewModel(
            robot = get(),
            mqttManager = get(),
            defaultMapName = androidApplication().getString(R.string.default_map_name),
            temiVoiceListener = get()
        )
    }
}
