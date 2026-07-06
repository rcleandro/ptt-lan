plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            implementation(project(":core:core-designsystem"))
            implementation(project(":core:core-navigation"))
            implementation(project(":core:core-audio"))
            implementation(project(":domain:domain-ptt"))
            implementation(project(":core:core-network"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
    }
}
