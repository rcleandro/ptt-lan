plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("ptt.compose.library")
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.pttlan.core.designsystem"
    compileSdkVersion(37)
    defaultConfig {
        minSdkVersion(26)
    }
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    jvm()
    
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            api(libs.ui)
            api(libs.foundation)
            api(libs.material3)
            api(libs.runtime)
            implementation(libs.material.icons.extended)
            api(libs.compose.components.resources)
            api(libs.compose.components.uiToolingPreview)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.pttlan.core.designsystem.generated.resources"
}
