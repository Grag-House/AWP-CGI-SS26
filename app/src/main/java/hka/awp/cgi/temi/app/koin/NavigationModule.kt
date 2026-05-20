package hka.awp.cgi.temi.app.koin

import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth
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
        // FIXME replace with correct IP
        val brokerHost = "192.168.178.31"
        val port = 1883

        val client = Mqtt5Client.builder()
            .identifier("temi-android-${UUID.randomUUID()}")
            .serverHost(brokerHost)
            .serverPort(port)
            .simpleAuth(
                Mqtt5SimpleAuth.builder()
                    // TODO move to .env
                    .username("mqtt")
                    .password("jch4ftjvgswzswirhzbojxgFGD".toByteArray())
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
            defaultMapName = androidApplication().getString(R.string.default_map_name)
        )
    }
}
