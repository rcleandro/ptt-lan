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
            implementation(project.dependencies.project(":core:core-di"))
            implementation(project.dependencies.project(":core:core-navigation"))
            implementation(project.dependencies.project(":core:core-designsystem"))
            implementation(project.dependencies.project(":features:feature-connection"))
            implementation(project.dependencies.project(":features:feature-channel-list"))
            implementation(project.dependencies.project(":features:feature-ptt"))
            implementation(project.dependencies.project(":features:feature-history"))
            implementation(project.dependencies.project(":features:feature-settings"))
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
            macOS {
                iconFile.set(project.file("src/main/resources/icon.icns"))
            }
            windows {
                iconFile.set(project.file("src/main/resources/icon.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/icon.png"))
            }
            packageName = "PTT"
            packageVersion = "1.0.0"
        }
    }
}
