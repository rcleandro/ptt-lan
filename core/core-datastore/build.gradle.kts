plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            api(libs.multiplatform.settings)
            api(libs.multiplatform.settings.coroutines)
        }
    }
}
