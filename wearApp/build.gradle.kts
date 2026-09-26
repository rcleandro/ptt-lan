plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
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
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // The phone's layers and components as they are; only the screens are the watch's own. No server-core:
    // the watch does not host (ADR 0011).
    implementation(projects.core.coreDi)
    implementation(projects.core.coreNavigation)
    implementation(projects.core.coreDesignsystem)
    implementation(projects.core.coreNetwork)
    implementation(projects.core.coreAudio)
    implementation(projects.domain.domainPtt)
    implementation(projects.features.featureConnection)
    implementation(projects.features.featureChannelList)
    implementation(projects.features.featurePtt)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.koin.android)
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.input)
    implementation(libs.wear.ongoing)
    implementation(libs.wear.tooling.preview)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
