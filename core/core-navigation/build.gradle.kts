plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(projects.core.coreDesignsystem)
            implementation(projects.core.coreNetwork)
            implementation(projects.domain.domainPtt)
            implementation(projects.features.featureConnection)
            implementation(projects.features.featureChannelList)
            implementation(projects.features.featurePtt)
            implementation(projects.features.featureHistory)
            implementation(projects.features.featureSettings)
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.material.icons.extended)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.koin.core)
        }
    }
}
