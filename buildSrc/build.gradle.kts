plugins {
    `kotlin-dsl`
}

repositories {
    google()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation(libs.plugin.agp)
    implementation(libs.plugin.kotlin)
    implementation(libs.plugin.compose.compiler)
    implementation(libs.plugin.compose)
    implementation(libs.plugin.ktlint)
    implementation(libs.plugin.detekt)
}
