plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":core:core-common"))
            api(libs.multiplatform.settings)
            api(libs.multiplatform.settings.coroutines)
        }
    }
}
