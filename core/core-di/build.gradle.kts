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
            api(project(":core:core-audio"))
            api(project(":domain:domain-ptt"))
            api(project(":data:data-ptt"))
            api(project(":features:feature-connection"))
            api(project(":features:feature-channel-list"))
            api(project(":features:feature-history"))
            api(project(":features:feature-ptt"))
            api(project(":features:feature-settings"))
            api(libs.koin.core)
        }
    }
}

dependencies {
    "androidMainApi"(libs.koin.android)
}
