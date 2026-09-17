plugins {
    id("ptt.kmp.library")
    alias(libs.plugins.serialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.serialization.protobuf)
            implementation(libs.koin.core)
            implementation(libs.kermit)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            // Reconnection is only meaningful against a real WSS endpoint, so the test boots a tiny Ktor server
            implementation(libs.ktor.server.netty)
            implementation(libs.ktor.server.websockets)
            implementation(libs.ktor.network.tls.certificates)
        }
        // Android and the JVM share the OkHttp client and its TLS rules, so they share a source set
        // instead of keeping two identical `actual` files (22.3).
        val jvmAndAndroidMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }
        jvmMain {
            dependsOn(jvmAndAndroidMain)
            dependencies {
                implementation(libs.jmdns)
            }
        }
        androidMain {
            dependsOn(jvmAndAndroidMain)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}
