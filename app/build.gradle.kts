import io.gitlab.arturbosch.detekt.Detekt
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dokka)
}

android {
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
        compileSdk = 36
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
    // runtime dependencies
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
    // temi dependency
    implementation(libs.temi.sdk)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.material.icons.extended)

    // unit test dependencies
    testImplementation(libs.mokk)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.params)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // debug depedencies
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // api desugaring
    coreLibraryDesugaring(libs.android.desugarJdkLibs)

    // linting and formatting
    detektPlugins(libs.detekt.ktlint)
    detektPlugins(libs.detekt.compose)

    // api call dependencies
    implementation(libs.okhttp)
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
