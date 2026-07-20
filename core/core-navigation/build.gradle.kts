plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
    kotlin("plugin.serialization") version "2.0.21"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":core:core-common"))
            implementation(project.dependencies.project(":core:core-designsystem"))
            implementation(project.dependencies.project(":core:core-network"))
            implementation(project.dependencies.project(":domain:domain-ptt"))
            implementation(project.dependencies.project(":features:feature-connection"))
            implementation(project.dependencies.project(":features:feature-channel-list"))
            implementation(project.dependencies.project(":features:feature-ptt"))
            implementation(project.dependencies.project(":features:feature-history"))
            implementation(project.dependencies.project(":features:feature-settings"))
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.material.icons.extended)
            implementation(libs.multiplatform.settings)
            implementation(libs.multiplatform.settings.coroutines)
            implementation(libs.koin.core)
        }
    }

    sourceSets.all {
        languageSettings.optIn("kotlin.uuid.ExperimentalUuidApi")
    }
}
