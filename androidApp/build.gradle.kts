plugins {
    alias(libs.plugins.androidApplication)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.pttlan.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.pttlan.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.core.coreDi)
    implementation(projects.core.coreNavigation)
    implementation(projects.core.coreDesignsystem)
    implementation(projects.features.featureConnection)
    implementation(projects.features.featureChannelList)
    implementation(projects.features.featurePtt)
    implementation(projects.features.featureHistory)
    implementation(projects.features.featureSettings)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
}
