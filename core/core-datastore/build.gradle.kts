plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
        }
    }
}
