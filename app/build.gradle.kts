import io.gitlab.arturbosch.detekt.Detekt
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.serialization)
}

android {
    signingConfigs {
        create("release") {
            val props = Properties()
            val envFile = rootProject.file(".env")
            if (envFile.exists()) {
                envFile.inputStream().use { props.load(it) }
                storeFile = file("$rootDir/release-key.jks")
                storePassword = props.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = props.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = props.getProperty("RELEASE_KEY_PASSWORD")
            }
        }
    }

    namespace = "hka.awp.cgi.temi.app"

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "hka.awp.cgi.temi.app"
        minSdk = 23
        // --> The App will only run on sdk 23 due to the limits of TEMI
        //noinspection OldTargetApi,ExpiredTargetSdkVersion
        targetSdk = 36
        compileSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // read from .env file
        val envFile = rootProject.file(".env")
        if (envFile.exists()) {
            val props = Properties()
            envFile.inputStream().use { props.load(it) }

            val webViewUrl =
                props.getProperty("WEBVIEW_URL") ?: throw GradleException("Missing property 'WEBVIEW_URL' in .env")
            buildConfigField("String", "WEBVIEW_URL", "\"$webViewUrl\"")

            val httpEnabledIpAddress = props.getProperty("HTTP_ALLOWED_IP")
                ?: throw GradleException("Missing property 'HTTP_ALLOWED_IP' in .env")
            buildConfigField("String", "HTTP_ALLOWED_IP", "\"$httpEnabledIpAddress\"")

            val mqttHost = props.getProperty("MQTT_HOST")
                ?: throw GradleException("Missing property 'MQTT_HOST' in .env")
            buildConfigField("String", "MQTT_HOST", "\"$mqttHost\"")

            val mqttPort = props.getProperty("MQTT_PORT")
                ?: throw GradleException("Missing property 'MQTT_PORT' in .env")
            buildConfigField("Integer", "MQTT_PORT", mqttPort)

            val mqttUsername = props.getProperty("MQTT_USERNAME")
                ?: throw GradleException("Missing property 'MQTT_USERNAME' in .env")
            buildConfigField("String", "MQTT_USERNAME", "\"$mqttUsername\"")

            val mqttPassword = props.getProperty("MQTT_PASSWORD")
                ?: throw GradleException("Missing property 'MQTT_PASSWORD' in .env")
            buildConfigField("String", "MQTT_PASSWORD", "\"$mqttPassword\"")

            val adminPassword = props.getProperty("DEFAULT_ADMIN_PASSWORD")
                ?: throw GradleException("Missing property 'DEFAULT_ADMIN_PASSWORD' in .env")
            buildConfigField("String", "DEFAULT_ADMIN_PASSWORD", "\"$adminPassword\"")

            val driveFolderLink = props.getProperty("DEFAULT_DRIVE_FOLDER_LINK")
                ?: throw GradleException("Missing property 'DEFAULT_DRIVE_FOLDER_LINK' in .env")
            buildConfigField("String", "DEFAULT_DRIVE_FOLDER_LINK", "\"$driveFolderLink\"")

            val driveUploadUrl = props.getProperty("DEFAULT_DRIVE_UPLOAD_URL")
                ?: throw GradleException("Missing property 'DEFAULT_DRIVE_UPLOAD_URL' in .env")
            buildConfigField("String", "DEFAULT_DRIVE_UPLOAD_URL", "\"$driveUploadUrl\"")
        } else {
            throw GradleException(
                "Missing .env file! please create it and include the 'WEBVIEW_URL"
            )
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            isDebuggable = false

            signingConfig = signingConfigs.getByName("release")
        }

        debug {
            isMinifyEnabled = false
            //noinspection NotShrinkingResources
            isShrinkResources = false
            isDebuggable = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    buildFeatures {
        compose = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // this is to exclude unwanted files from the resulting package
    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = false
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
}

tasks.withType<Detekt>().configureEach {
    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt-report.html"))

        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/detekt/detekt-report.xml"))

        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+PropertyParamAnnotationDefaultTargetMode")
    }
}

dependencies {
    implementation(libs.androidx.ui)
    implementation(libs.firebase.annotations)
    implementation(libs.koin.android)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.koin.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.text.google.fonts)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.timber)
    implementation(libs.androidx.datastore.preferences)

    // mqtt
    implementation(libs.hivemq)

    // api call dependencies
    implementation(libs.okhttp)
    implementation(libs.okhttp.tls)
    implementation(libs.kotlinx.serialization.json)

    // temi dependency
    implementation(libs.temi.sdk)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.material.icons.extended)

    // Vosk speech recognition dependencies
    implementation(libs.vosk.android)

    // ----------------- DEBUG / Compile time dependencies -----------------------

    // unit test dependencies
    testImplementation(libs.mockk)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.params)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    // debug dependencies
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // QR code generation for Photobox photo links
    implementation(libs.zxing.core)

    // Offline upload retry queue for Photobox (survives app restarts and waits for network)
    implementation(libs.androidx.work.runtime.ktx)

    // api desugaring
    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    // linting and formatting
    detektPlugins(libs.detekt.ktlint)
    detektPlugins(libs.detekt.compose)
}

tasks.withType<Detekt>().configureEach {
    if (project.hasProperty("autoFormat")) {
        autoCorrect = true
        println("🛠️ Detekt Auto-Correct enabled!")
    } else {
        println("🔍 Detekt running in read only mode!")
    }
}

tasks.register("qualityCheck") {
    group = "verification"
    description = "Run detect analysis and create dokka-documentation."

    dependsOn("detekt")
    dependsOn("dokkaGenerate")

    tasks.findByName("dokkaGenerate")?.mustRunAfter("detekt")
}

tasks.register<Sync>("copyDokkaReports") {
    group = "documentation"
    description = "Copy the dokka reports to the assets folder"

    dependsOn("dokkaGenerate")

    println("Copying dokka reports!")
    from(layout.buildDirectory.dir("dokka/html"))
    into(layout.projectDirectory.dir("src/main/assets/html"))
}
