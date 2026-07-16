package hka.awp.cgi.temi.app.koin

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.auth.Mqtt5SimpleAuth
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.data.repository.GeneralConfigRepository
import hka.awp.cgi.temi.app.data.repository.PatrolConfigRepository
import hka.awp.cgi.temi.app.data.repository.PhotoboxConfigRepository
import hka.awp.cgi.temi.app.data.repository.RobotRepository
import hka.awp.cgi.temi.app.data.repository.SecurityConfigRepository
import hka.awp.cgi.temi.app.feature.mqtt.MqttManager
import hka.awp.cgi.temi.app.utils.NetworkManager
import hka.awp.cgi.temi.app.utils.TemiBatteryMonitor
import hka.awp.cgi.temi.app.utils.TemiMovementController
import hka.awp.cgi.temi.app.utils.security.PasswordHasher
import hka.awp.cgi.temi.app.utils.security.Sha256PasswordHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.decodeCertificatePem
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import timber.log.Timber
import java.time.Clock
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "webserver_settings")

val coreModule = module {
    // Shared Utilities
    single<NetworkManager> { NetworkManager(androidContext()) }
    single<Clock> { Clock.systemDefaultZone() }
    single<DateTimeFormatter> {
        DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }

    // Data Storage
    single<DataStore<Preferences>> { androidContext().appDataStore }
    single { GeneralConfigRepository(dataStore = get(), context = androidContext()) }
    single { PatrolConfigRepository(dataStore = get()) }
    single { PhotoboxConfigRepository(dataStore = get()) }
    single<PasswordHasher> { Sha256PasswordHasher() }
    single { SecurityConfigRepository(dataStore = get(), passwordHasher = get()) }

    // Robot & Temi SDK
    single { RobotRepository() }
    single<Robot?> {
        try {
            Robot.getInstance()
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            Timber.e(e, "Temi SDK not available, probably running locally")
            null
        }
    }
    single { TemiBatteryMonitor(robot = get(), mqttManager = get()) }
    single { TemiMovementController(robot = get(), scope = get()) }

    // Networking
    single<OkHttpClient> {
        // This is needed, because the Temi root certs expired in 2021 and valid until: 2/13/45, 11:55:37 GMT+1
        val rootCert = """
            -----BEGIN CERTIFICATE-----
            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
            63ZTGI0RmLo=
            -----END CERTIFICATE-----
        """.trimIndent()

        val certificates = HandshakeCertificates.Builder()
            .addTrustedCertificate(rootCert.decodeCertificatePem())
            .addPlatformTrustedCertificates()
            .build()

        OkHttpClient.Builder()
            .sslSocketFactory(certificates.sslSocketFactory(), certificates.trustManager)
            .build()
    }

    // MQTT
    single {
        Mqtt5Client.builder()
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
    }
    single { MqttManager(client = get(), robot = get()) }
}
