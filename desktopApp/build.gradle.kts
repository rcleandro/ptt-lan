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
            implementation(project(":core:core-designsystem"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
    }
}
