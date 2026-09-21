package com.pttlan.android

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val REGISTRATION_TIMEOUT_S = 5L

/**
 * Announces a hosted server as `_pttlan._tcp` through Android's NSD — the counterpart of the JmDNS
 * `announceOnLan` of `server-core`, which on Android would need a multicast lock and its own responder.
 * Blocks until NSD confirms; the handle unregisters. Null when registration failed.
 */
fun announceWithNsd(
    context: Context,
    port: Int,
    serviceName: String,
): AutoCloseable? {
    val nsd = context.getSystemService(NsdManager::class.java)
    val registered = CountDownLatch(1)
    var failed = false
    val listener =
        object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) = registered.countDown()

            override fun onRegistrationFailed(
                info: NsdServiceInfo,
                errorCode: Int,
            ) {
                Log.w("NsdAnnouncer", "NSD registration failed: $errorCode")
                failed = true
                registered.countDown()
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) = Unit

            override fun onUnregistrationFailed(
                info: NsdServiceInfo,
                errorCode: Int,
            ) = Unit
        }
    val info =
        NsdServiceInfo().apply {
            this.serviceName = serviceName
            serviceType = "_pttlan._tcp"
            this.port = port
        }
    nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, listener)
    if (!registered.await(REGISTRATION_TIMEOUT_S, TimeUnit.SECONDS) || failed) return null
    return AutoCloseable { nsd.unregisterService(listener) }
}
