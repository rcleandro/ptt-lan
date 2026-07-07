plugins {
    id("ptt.compose.library")
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.multiplatformLibrary)
    alias(libs.plugins.roborazzi)
}

kotlin {
    android {
        namespace = "com.pttlan.core.designsystem"
        compileSdk = 37
        minSdk = 26
        withHostTest {
            isIncludeAndroidResources = true
        }
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
        val androidHostTest = sourceSets.getByName("androidHostTest")
        androidHostTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.robolectric)
            implementation(libs.roborazzi)
            implementation(libs.roborazzi.compose)
            implementation(libs.roborazzi.junit)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.junit)
            implementation(libs.androidx.ui.test.junit4)
            implementation(libs.androidx.ui.test.manifest)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.pttlan.core.designsystem.generated.resources"
}
