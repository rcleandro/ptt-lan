plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            implementation(project(":core:core-designsystem"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(project(":core:core-navigation"))
            implementation(project(":core:core-datastore"))
            implementation(project(":domain:domain-ptt"))

            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
    }
}
