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
        minSdk = 24
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
    implementation(project(":core:core-designsystem"))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
}
