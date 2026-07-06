plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:core-common"))
            api(project(":core:core-network"))
            api(project(":core:core-database"))
            api(project(":core:core-datastore"))
            api(project(":core:core-telemetry"))
            api(libs.koin.core)
        }
        androidMain.dependencies {
            api(libs.koin.android)
        }
    }
}
