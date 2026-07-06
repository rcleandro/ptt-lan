import os

# serverApp
os.makedirs("serverApp/src/main/kotlin/com/pttlan/server", exist_ok=True)
with open("serverApp/build.gradle.kts", "w") as f:
    f.write("""plugins {
    kotlin("jvm")
    application
}
application {
    mainClass.set("com.pttlan.server.ApplicationKt")
}
""")
with open("serverApp/src/main/kotlin/com/pttlan/server/Application.kt", "w") as f:
    f.write("""package com.pttlan.server
fun main() {
    println("Hello PTT-LAN")
}
""")

# desktopApp
os.makedirs("desktopApp/src/jvmMain/kotlin/com/pttlan/desktop", exist_ok=True)
with open("desktopApp/build.gradle.kts", "w") as f:
    f.write("""plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}
kotlin {
    jvm()
}
""")
with open("desktopApp/src/jvmMain/kotlin/com/pttlan/desktop/Main.kt", "w") as f:
    f.write("""package com.pttlan.desktop
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.material.Text

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "PTT-LAN") {
        Text("Hello PTT-LAN")
    }
}
""")

# androidApp
os.makedirs("androidApp/src/main/kotlin/com/pttlan/android", exist_ok=True)
with open("androidApp/build.gradle.kts", "w") as f:
    f.write("""plugins {
    id("com.android.application")
    kotlin("android")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.pttlan.android"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.pttlan.android"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}
""")
with open("androidApp/src/main/AndroidManifest.xml", "w") as f:
    f.write("""<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="PTT-LAN"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
""")
with open("androidApp/src/main/kotlin/com/pttlan/android/MainActivity.kt", "w") as f:
    f.write("""package com.pttlan.android
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Text("Hello PTT-LAN")
        }
    }
}
""")

print("Apps configured.")
