package com.pttlan.core.network.discovery

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.get
import kotlinx.cinterop.reinterpret
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSData
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.darwin.NSObject
import platform.posix.AF_INET

actual class ServerDiscoveryService actual constructor() {
    private var browser: NSNetServiceBrowser? = null

    actual fun discover(): Flow<DiscoveredServer> =
        callbackFlow {
            val serviceType = "_pttlan._tcp."
            val domain = "local."

            val services = mutableMapOf<String, NSNetService>()

            val browserDelegate =
                object : NSObject(), NSNetServiceBrowserDelegateProtocol {
                    @ObjCSignatureOverride
                    override fun netServiceBrowser(
                        browser: NSNetServiceBrowser,
                        didFindService: NSNetService,
                        moreComing: Boolean,
                    ) {
                        services[didFindService.name] = didFindService

                        val serviceDelegate =
                            object : NSObject(), NSNetServiceDelegateProtocol {
                                override fun netServiceDidResolveAddress(sender: NSNetService) {
                                    val host = sender.ipv4Address() ?: sender.hostName ?: return
                                    val server =
                                        DiscoveredServer(
                                            name = sender.name,
                                            host = host,
                                            port = sender.port.toInt(),
                                        )
                                    trySend(server)
                                }

                                @ObjCSignatureOverride
                                override fun netService(
                                    sender: NSNetService,
                                    didNotResolve: Map<Any?, *>,
                                ) {
                                    // Handle resolve failure
                                }
                            }

                        didFindService.delegate = serviceDelegate
                        didFindService.resolveWithTimeout(5.0)
                    }

                    @ObjCSignatureOverride
                    override fun netServiceBrowser(
                        browser: NSNetServiceBrowser,
                        didRemoveService: NSNetService,
                        moreComing: Boolean,
                    ) {
                        services.remove(didRemoveService.name)
                    }
                }

            browser =
                NSNetServiceBrowser().apply {
                    delegate = browserDelegate
                    searchForServicesOfType(serviceType, domain)
                }

            awaitClose {
                stopDiscovery()
            }
        }

    actual fun stopDiscovery() {
        browser?.stop()
        browser = null
    }
}

// Darwin sockaddr_in: sin_len, sin_family, sin_port (2 bytes), then the 4 address bytes.
private const val FAMILY_OFFSET = 1
private const val ADDRESS_OFFSET = 4
private const val ADDRESS_BYTES = 4

/**
 * The IPv4 the resolution already found. Connecting by `hostName` meant a second mDNS lookup on every attempt,
 * and the name Android's NSD announces (`Android_XXXX.local`) rarely resolves, so iOS needed several tries.
 */
@OptIn(ExperimentalForeignApi::class)
private fun NSNetService.ipv4Address(): String? =
    addresses.orEmpty().filterIsInstance<NSData>().firstNotNullOfOrNull { data ->
        val bytes = data.bytes?.reinterpret<UByteVar>()
        val end = ADDRESS_OFFSET + ADDRESS_BYTES
        if (bytes == null || data.length < end.toULong() || bytes[FAMILY_OFFSET].toInt() != AF_INET) {
            null
        } else {
            (ADDRESS_OFFSET until end).joinToString(".") { bytes[it].toString() }
        }
    }
