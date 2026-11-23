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
            api(projects.domain.domainPtt)
            api(libs.decompose)
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
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// SwiftStringKeysTest reads the Swift screens: without this, editing only Swift would leave the test up to date
tasks.named<Test>("jvmTest") {
    inputs.dir(rootDir.resolve("iosApp/iosApp")).withPathSensitivity(PathSensitivity.RELATIVE)
    inputs.file(rootDir.resolve("core/core-designsystem/src/commonMain/composeResources/values/strings.xml"))
}
