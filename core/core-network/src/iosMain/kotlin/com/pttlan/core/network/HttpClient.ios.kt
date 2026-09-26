package com.pttlan.core.network

import com.pttlan.core.common.network.isLocalNetwork
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.convert
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.create
import platform.Foundation.serverTrust
import platform.Security.SecCertificateCopyData
import platform.Security.SecCertificateRef
import platform.Security.SecTrustCopyCertificateChain
import platform.Security.SecTrustRef

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
actual fun createPlatformHttpClient(pins: CertificatePins): HttpClient =
    HttpClient(Darwin) {
        engine {
            handleChallenge { _, _, challenge, completionHandler ->
                val serverTrust = challenge.protectionSpace.serverTrust
                // Self-signed certificates are only accepted on the LAN, the same rule Android and JVM apply.
                // Anywhere else the system validation runs, so a MITM on the internet is rejected.
                val host = challenge.protectionSpace.host
                if (serverTrust != null && isLocalNetwork(host)) {
                    // Trusted on first use, refused if the certificate changes (30.5)
                    val fingerprint = leafFingerprint(serverTrust)
                    if (fingerprint != null && pins.verify(host, challenge.protectionSpace.port.toInt(), fingerprint)) {
                        completionHandler(NSURLSessionAuthChallengeUseCredential.convert(), NSURLCredential.create(serverTrust))
                    } else {
                        completionHandler(NSURLSessionAuthChallengeCancelAuthenticationChallenge.convert(), null)
                    }
                } else {
                    completionHandler(NSURLSessionAuthChallengePerformDefaultHandling.convert(), null)
                }
            }
        }
    }

/** SHA-256 of the certificate the server presented, the first in its chain. */
@OptIn(ExperimentalForeignApi::class)
private fun leafFingerprint(trust: SecTrustRef): String? {
    val chain = SecTrustCopyCertificateChain(trust) ?: return null
    return try {
        val leaf: SecCertificateRef? = if (CFArrayGetCount(chain) > 0) CFArrayGetValueAtIndex(chain, 0)?.reinterpret() else null
        leaf?.let(::SecCertificateCopyData)?.let { data ->
            try {
                CFDataGetBytePtr(data)?.readBytes(CFDataGetLength(data).toInt())?.let(::sha256Hex)
            } finally {
                CFRelease(data)
            }
        }
    } finally {
        CFRelease(chain)
    }
}
