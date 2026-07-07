plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
    id("ptt.compose.library")
}

kotlin {
    androidLibrary {
        namespace = "com.pttlan.core.designsystem"
        compileSdk = 37
        minSdk = 26
    }
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
