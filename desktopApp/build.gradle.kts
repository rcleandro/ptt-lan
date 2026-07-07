plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
kotlin {
    jvm()
    sourceSets {
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(project(":core:core-di"))
            implementation(project(":core:core-navigation"))
            implementation(project(":core:core-designsystem"))
            implementation(project(":features:feature-connection"))
            implementation(project(":features:feature-channel-list"))
            implementation(project(":features:feature-ptt"))
            implementation(project(":features:feature-history"))
            implementation(project(":features:feature-settings"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.pttlan.desktop.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb,
            )
            packageName = "PTT"
            packageVersion = "1.0.0"
        }
    }
}
