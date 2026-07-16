plugins {
    alias(libs.plugins.androidApplication)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

@Suppress("DEPRECATION")
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
    implementation(project(":core:core-di"))
    implementation(project(":core:core-navigation"))
    implementation(project(":core:core-designsystem"))
    implementation(project(":features:feature-connection"))
    implementation(project(":features:feature-channel-list"))
    implementation(project(":features:feature-ptt"))
    implementation(project(":features:feature-history"))
    implementation(project(":features:feature-settings"))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.ui.tooling.preview)
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
}
