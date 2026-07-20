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
            implementation(project.dependencies.project(":core:core-common"))
            api(libs.ui)
            api(libs.foundation)
            api(libs.material3)
            api(libs.runtime)
            implementation(libs.material.icons.extended)
            api(libs.compose.components.resources)
            api(libs.ui.tooling.preview)
        }
    }
}

dependencies {
    "androidMainApi"(libs.ui.tooling)
    "androidHostTestImplementation"(kotlin("test"))
    "androidHostTestImplementation"(libs.robolectric)
    "androidHostTestImplementation"(libs.roborazzi)
    "androidHostTestImplementation"(libs.roborazzi.compose)
    "androidHostTestImplementation"(libs.roborazzi.junit)
    "androidHostTestImplementation"(libs.ui.tooling.preview)
    "androidHostTestImplementation"(libs.androidx.junit)
    "androidHostTestImplementation"(libs.androidx.ui.test.junit4)
    "androidHostTestImplementation"(libs.androidx.ui.test.manifest)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "com.pttlan.core.designsystem.generated.resources"
}
