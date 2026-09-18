plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.serialization)
    application
    alias(libs.plugins.kover)
}
application {
    mainClass.set("com.pttlan.server.ApplicationKt")
}

dependencies {
    implementation(projects.core.coreCommon)
    implementation(projects.core.coreNetwork)
    implementation(libs.jmdns)

    // Server dependencies
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.host.common)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.forwarded.header)

    implementation(libs.koin.core)
    implementation(libs.koin.ktor)
    implementation(libs.ktor.network.tls.certificates)
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}
