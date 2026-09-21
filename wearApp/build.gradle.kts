plugins {
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.pttlan.wear"
    compileSdk = 37
    defaultConfig {
        // Same id as the phone app: Wear OS pairs the two as one product (ADR 0011)
        applicationId = "com.pttlan.android"
        minSdk = 30
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.core.coreNetwork)
    implementation(projects.core.coreAudio)
    implementation(libs.kotlinx.coroutines.core)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
