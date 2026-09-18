package com.pttlan.server

import io.ktor.network.tls.certificates.generateCertificate
import io.ktor.server.netty.EngineMain
import java.io.File

fun main(args: Array<String>) {
    val keyStoreFile = File("build/keystore.jks")
    if (!keyStoreFile.exists()) {
        // The same password must reach `ktor.security.ssl` in application.conf, hence the shared env var
        val keyStorePassword = System.getenv("PTT_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() } ?: "password"
        keyStoreFile.parentFile?.mkdirs()
        generateCertificate(
            keyStoreFile,
            keyAlias = "pttlan",
            keyPassword = keyStorePassword,
            jksPassword = keyStorePassword,
        )
    }

    Thread {
        announceOnLan(PTT_PORT, "PTT-LAN-Server-${System.currentTimeMillis()}")?.let { announcement ->
            Runtime.getRuntime().addShutdownHook(Thread { announcement.close() })
        }
    }.start()
    EngineMain.main(args)
}
