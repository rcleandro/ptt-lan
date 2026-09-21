plugins {
    alias(libs.plugins.kotlinJvm)
    application
    alias(libs.plugins.kover)
}
application {
    mainClass.set("com.pttlan.server.ApplicationKt")
}

dependencies {
    implementation(projects.serverCore)

    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.network.tls.certificates)
    implementation(libs.logback.classic)
}
