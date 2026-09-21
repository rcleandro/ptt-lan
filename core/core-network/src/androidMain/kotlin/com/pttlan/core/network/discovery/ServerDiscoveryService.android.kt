package com.pttlan.core.network.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual class ServerDiscoveryService actual constructor() : KoinComponent {
    private val context: Context by inject()
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    actual fun discover(): Flow<DiscoveryEvent> =
        callbackFlow {
            val serviceType = "_pttlan._tcp."

            discoveryListener =
                object : NsdManager.DiscoveryListener {
                    override fun onStartDiscoveryFailed(
                        serviceType: String,
                        errorCode: Int,
                    ) {
                        close(Exception("Discovery failed with error code $errorCode"))
                    }

                    override fun onStopDiscoveryFailed(
                        serviceType: String,
                        errorCode: Int,
                    ) {
                        // Ignore
                    }

                    override fun onDiscoveryStarted(serviceType: String) {}

                    override fun onDiscoveryStopped(serviceType: String) {}

                    @Suppress("DEPRECATION")
                    override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                        nsdManager.resolveService(
                            serviceInfo,
                            object : NsdManager.ResolveListener {
                                override fun onResolveFailed(
                                    failed: NsdServiceInfo,
                                    errorCode: Int,
                                ) {}

                                override fun onServiceResolved(resolved: NsdServiceInfo) {
                                    val host = resolved.host?.hostAddress ?: return
                                    val server =
                                        DiscoveredServer(
                                            name = resolved.serviceName,
                                            host = host,
                                            port = resolved.port,
                                        )
                                    trySend(DiscoveryEvent.Found(server))
                                }
                            },
                        )
                    }

                    override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                        trySend(DiscoveryEvent.Lost(serviceInfo.serviceName))
                    }
                }

            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)

            awaitClose {
                stopDiscovery()
            }
        }

    actual fun stopDiscovery() {
        discoveryListener?.let {
            try {
                nsdManager.stopServiceDiscovery(it)
            } catch (_: Exception) {
                // Ignore if already stopped
            }
        }
        discoveryListener = null
    }
}
