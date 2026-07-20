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
    implementation("io.ktor:ktor-server-core:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-host-common:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-netty:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-websockets:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.get()}")
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.lettuce.core)

    implementation(libs.koin.core)
    implementation("io.insert-koin:koin-ktor:${libs.versions.koin.get()}")
    implementation("io.ktor:ktor-network-tls-certificates:${libs.versions.ktor.get()}")
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(kotlin("test"))
}
