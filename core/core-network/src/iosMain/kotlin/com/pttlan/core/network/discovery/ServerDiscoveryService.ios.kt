package com.pttlan.core.network.discovery

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import platform.Foundation.NSNetService
import platform.Foundation.NSNetServiceBrowser
import platform.Foundation.NSNetServiceBrowserDelegateProtocol
import platform.Foundation.NSNetServiceDelegateProtocol
import platform.darwin.NSObject

actual class ServerDiscoveryService actual constructor() {
    private var browser: NSNetServiceBrowser? = null

    actual fun discover(): Flow<DiscoveredServer> = callbackFlow {
        val serviceType = "_pttlan._tcp."
        val domain = "local."
        
        val services = mutableMapOf<String, NSNetService>()
        
        val browserDelegate = object : NSObject(), NSNetServiceBrowserDelegateProtocol {
            override fun netServiceBrowser(
                browser: NSNetServiceBrowser,
                didFindService: NSNetService,
                moreComing: Boolean
            ) {
                services[didFindService.name] = didFindService
                
                val serviceDelegate = object : NSObject(), NSNetServiceDelegateProtocol {
                    override fun netServiceDidResolveAddress(sender: NSNetService) {
                        val host = sender.hostName ?: return
                        val server = DiscoveredServer(
                            name = sender.name,
                            host = host,
                            port = sender.port.toInt()
                        )
                        trySend(server)
                    }
                    
                    override fun netService(sender: NSNetService, didNotResolve: platform.Foundation.NSDictionary) {
                        // Handle resolve failure
                    }
                }
                
                didFindService.delegate = serviceDelegate
                didFindService.resolveWithTimeout(5.0)
            }

            override fun netServiceBrowser(
                browser: NSNetServiceBrowser,
                didRemoveService: NSNetService,
                moreComing: Boolean
            ) {
                services.remove(didRemoveService.name)
            }
        }

        browser = NSNetServiceBrowser().apply {
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
