plugins {
    id("ptt.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            freeCompilerArgs += listOf("-Xbinary=bundleId=com.pttlan.shared")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-di"))
            implementation(project(":core:core-designsystem"))
            implementation(project(":core:core-navigation"))
            implementation(project(":features:feature-connection"))
            implementation(project(":features:feature-channel-list"))
            implementation(project(":features:feature-ptt"))
            implementation(project(":features:feature-history"))
            implementation(project(":features:feature-settings"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.ui.tooling.preview)
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
    }
}
