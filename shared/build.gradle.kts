plugins {
    id("ptt.kmp.library")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
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
            
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
    }
}
