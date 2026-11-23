plugins {
    id("com.android.library")
}

android {
    namespace = "com.pttlan" + project.path.replace(":", ".").replace("-", "")
    compileSdk { version = release(37) { minorApiLevel = 1 } }
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
