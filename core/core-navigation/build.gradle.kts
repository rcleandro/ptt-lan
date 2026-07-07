plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
    kotlin("plugin.serialization") version "2.0.21"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            implementation(project(":core:core-designsystem"))
            implementation(project(":core:core-network"))
            implementation(project(":domain:domain-ptt"))
            implementation(project(":features:feature-connection"))
            implementation(project(":features:feature-channel-list"))
            implementation(project(":features:feature-ptt"))
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.material.icons.extended)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
    }
}
