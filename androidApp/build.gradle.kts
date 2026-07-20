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
    implementation(project.dependencies.project(":core:core-di"))
    implementation(project.dependencies.project(":core:core-navigation"))
    implementation(project.dependencies.project(":core:core-designsystem"))
    implementation(project.dependencies.project(":features:feature-connection"))
    implementation(project.dependencies.project(":features:feature-channel-list"))
    implementation(project.dependencies.project(":features:feature-ptt"))
    implementation(project.dependencies.project(":features:feature-history"))
    implementation(project.dependencies.project(":features:feature-settings"))
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
}
