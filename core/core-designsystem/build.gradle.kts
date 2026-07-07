plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
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
