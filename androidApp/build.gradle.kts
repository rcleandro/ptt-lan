plugins {
    alias(libs.plugins.androidApplication)
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
}
