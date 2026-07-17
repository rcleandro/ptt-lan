plugins {
    alias(libs.plugins.kotlinJvm)
    application
}
application {
    mainClass.set("com.pttlan.server.ApplicationKt")
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(libs.jmdns)

    // Server dependencies
    implementation("io.ktor:ktor-server-core:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-host-common:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-netty:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-websockets:${libs.versions.ktor.get()}")
    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.get()}")
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.koin.core)
    implementation("io.insert-koin:koin-ktor:${libs.versions.koin.get()}")
    implementation("io.ktor:ktor-network-tls-certificates:${libs.versions.ktor.get()}")
    implementation(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(kotlin("test"))
}
