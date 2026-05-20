# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- TEMI ROBOT SDK ---
# keep proguard from touching the Temi SDK
-keep class com.robotemi.sdk.** { *; }
-keep interface com.robotemi.sdk.** { *; }
-keep enum com.robotemi.sdk.** { *; }


# HiveMQ & Netty exceptions for release mode
# Prevents R8 from removing methods that are accessed via reflection
# ------------------------------------------------------------------
# Keep Netty (the network foundation) completely intact
-keep class io.netty.** { *; }
-dontwarn io.netty.**
# Protect the HiveMQ client from obfuscation as well
-keep class com.hivemq.client.** { *; }
-dontwarn com.hivemq.client.**
# JCTools is used internally by HiveMQ and resolves fields by exact name via Unsafe.
# Do not obfuscate/shrink it, otherwise release builds can crash with NoSuchFieldException.
-keep class org.jctools.** { *; }
-keepnames class org.jctools.**
-dontwarn org.jctools.**
-keep class hka.awp.cgi.temi.app.feature.mqtt.MqttCommand { *; }
-keep class hka.awp.cgi.temi.app.feature.mqtt.MqttStatus { *; }
