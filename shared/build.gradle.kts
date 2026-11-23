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
            // Types the SwiftUI screens use, under their own names instead of prefixed ones (32.1)
            export(projects.core.coreNavigation)
            export(projects.features.featureConnection)
            export(projects.features.featureChannelList)
            export(projects.features.featurePtt)
            export(projects.features.featureSettings)
            export(projects.features.featureHistory)
            export(projects.domain.domainPtt)
            export(libs.decompose)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreDi)
            implementation(projects.core.coreDesignsystem)
            api(projects.core.coreNavigation)
            api(projects.features.featureConnection)
            api(projects.features.featureChannelList)
            api(projects.features.featurePtt)
            api(projects.features.featureSettings)
            api(projects.features.featureHistory)
            api(projects.domain.domainPtt)
            api(libs.decompose)
            // Colors (toArgb) and sizes (Dp) of the design tokens; no Compose screen runs on iOS (32.6)
            implementation(libs.ui)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// SwiftStringKeysTest and WatchPaletteTest read the Swift sources: without this, editing only Swift would leave the test up to date
tasks.named<Test>("jvmTest") {
    inputs.dir(rootDir.resolve("iosApp/iosApp")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.dir(rootDir.resolve("iosApp/watchApp")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(rootDir.resolve("core/core-designsystem/src/commonMain/composeResources/values/strings.xml"))
}
