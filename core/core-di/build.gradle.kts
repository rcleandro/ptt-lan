plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project.dependencies.project(":core:core-common"))
            api(project.dependencies.project(":core:core-network"))
            api(project.dependencies.project(":core:core-database"))
            api(project.dependencies.project(":core:core-datastore"))
            api(project.dependencies.project(":core:core-telemetry"))
            api(project.dependencies.project(":core:core-audio"))
            api(project.dependencies.project(":domain:domain-ptt"))
            api(project.dependencies.project(":data:data-ptt"))
            api(project.dependencies.project(":features:feature-connection"))
            api(project.dependencies.project(":features:feature-channel-list"))
            api(project.dependencies.project(":features:feature-history"))
            api(project.dependencies.project(":features:feature-ptt"))
            api(project.dependencies.project(":features:feature-settings"))
            api(libs.koin.core)
        }
    }
}

dependencies {
    "androidMainApi"(libs.koin.android)
}
