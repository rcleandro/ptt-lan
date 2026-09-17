package com.pttlan.core.network

import com.pttlan.core.common.network.isLocalNetwork
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create
import platform.Foundation.serverTrust

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun createPlatformHttpClient(): HttpClient =
    HttpClient(Darwin) {
        engine {
            handleChallenge { _, _, challenge, completionHandler ->
                val serverTrust = challenge.protectionSpace.serverTrust
                // Self-signed certificates are only accepted on the LAN, the same rule Android and JVM apply.
                // Anywhere else the system validation runs, so a MITM on the internet is rejected.
                if (serverTrust != null && isLocalNetwork(challenge.protectionSpace.host)) {
                    val credential = NSURLCredential.create(serverTrust)
                    completionHandler(NSURLSessionAuthChallengeUseCredential.convert(), credential)
                } else {
                    completionHandler(NSURLSessionAuthChallengePerformDefaultHandling.convert(), null)
                }
            }
        }
    }
