plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.baselineProfile)
}

/**
 * Generates the app's Baseline Profile (31.3) on a connected device:
 * `./gradlew :androidApp:generateBaselineProfile`. The result lands in `androidApp/src/release/generated` and is
 * committed; regenerate it when the startup or the PTT screen change.
 * The run reinstalls the app on the device, which wipes its data (nickname, trusted servers, history).
 */
android {
    namespace = "com.pttlan.baselineprofile"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    targetProjectPath = ":androidApp"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.uiautomator)
    constraints {
        implementation(libs.wire.runtime) {
            because("Macrobenchmark brings 6.4.0, vulnerable to GHSA-9rm7-3qhh-h2mc")
        }
    }
}
