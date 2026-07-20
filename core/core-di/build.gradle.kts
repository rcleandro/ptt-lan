plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.coreCommon)
            api(projects.core.coreNetwork)
            api(projects.core.coreDatabase)
            api(projects.core.coreDatastore)
            api(projects.core.coreTelemetry)
            api(projects.core.coreAudio)
            api(projects.domain.domainPtt)
            api(projects.data.dataPtt)
            api(projects.features.featureConnection)
            api(projects.features.featureChannelList)
            api(projects.features.featureHistory)
            api(projects.features.featurePtt)
            api(projects.features.featureSettings)
            api(libs.koin.core)
        }
    }
}

dependencies {
    "androidMainApi"(libs.koin.android)
}
