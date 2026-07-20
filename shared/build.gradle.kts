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
            implementation(project.dependencies.project(":core:core-di"))
            implementation(project.dependencies.project(":core:core-designsystem"))
            implementation(project.dependencies.project(":core:core-navigation"))
            implementation(project.dependencies.project(":features:feature-connection"))
            implementation(project.dependencies.project(":features:feature-channel-list"))
            implementation(project.dependencies.project(":features:feature-ptt"))
            implementation(project.dependencies.project(":features:feature-history"))
            implementation(project.dependencies.project(":features:feature-settings"))
            implementation(libs.runtime)
            implementation(libs.foundation)
            implementation(libs.material3)
            implementation(libs.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.components.uiToolingPreview)
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
    }
}
