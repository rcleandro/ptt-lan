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
            implementation(projects.core.coreDi)
            implementation(projects.core.coreDesignsystem)
            implementation(projects.core.coreNavigation)
            implementation(projects.features.featureConnection)
            implementation(projects.features.featureChannelList)
            implementation(projects.features.featurePtt)
            implementation(projects.features.featureHistory)
            implementation(projects.features.featureSettings)
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
