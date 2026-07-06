plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain:domain-ptt"))
            implementation(project(":core:core-designsystem"))
            implementation(project(":core:core-navigation"))
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.koin.compose)
        }
    }
}
