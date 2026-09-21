package com.pttlan.wear

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wear OS reaches the internet through the phone over Bluetooth and keeps Wi-Fi off, and that route does not
 * reach the LAN. This asks for Wi-Fi and binds the whole process to it while anyone holds it (spike 25.1).
 * Counted, because the screen and the session hold it on their own: the binding is process-wide, and one of
 * them letting go must not cut the other off.
 */
class LanNetwork(
    context: Context,
) {
    private val connectivity = context.getSystemService(ConnectivityManager::class.java)
    private val _available = MutableStateFlow(false)
    val available: StateFlow<Boolean> = _available.asStateFlow()
    private var holders = 0

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivity.bindProcessToNetwork(network)
                _available.value = true
            }

            override fun onLost(network: Network) {
                connectivity.bindProcessToNetwork(null)
                _available.value = false
            }
        }

    @Synchronized
    fun acquire() {
        if (holders++ == 0) {
            connectivity.requestNetwork(
                NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(),
                callback,
            )
        }
    }

    @Synchronized
    fun release() {
        if (holders == 0) return
        if (--holders == 0) {
            connectivity.bindProcessToNetwork(null)
            connectivity.unregisterNetworkCallback(callback)
            _available.value = false
        }
    }
}
