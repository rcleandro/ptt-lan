package com.pttlan.wear

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import org.koin.android.ext.android.inject
import com.pttlan.domain.ptt.repository.ConnectionStatus
import com.pttlan.domain.ptt.repository.ConnectionRepository
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import android.content.Intent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.retainedComponent
import com.pttlan.core.navigation.RootComponent
import com.pttlan.feature.connection.ConnectionIntent
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/** Android 17, where the local network needs a runtime permission (found in spike 25.1). */
private const val LOCAL_NETWORK_PERMISSION_SDK = 37

class MainActivity : ComponentActivity() {
    private val lanNetwork: LanNetwork by inject()
    private val connectionRepository: ConnectionRepository by inject()
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { showApp() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        lanNetwork.acquire()

        val missing =
            buildList {
                add(Manifest.permission.RECORD_AUDIO)
                // The session's Ongoing Activity is a notification
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
                if (Build.VERSION.SDK_INT >= LOCAL_NETWORK_PERMISSION_SDK) add(Manifest.permission.ACCESS_LOCAL_NETWORK)
            }.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) {
            showApp()
        } else {
            // Ask before anything searches the network: a search without the permission opens a system picker
            // on top of the permission prompt instead of listing the rooms
            setContent { WearPermissionWait() }
            requestPermissions.launch(missing.toTypedArray())
        }
    }

    private fun showApp() {
        val root = retainedComponent { RootComponent(componentContext = it) }
        lifecycleScope.launch {
            // The first search can start before Wi-Fi is up; search again once the LAN is reachable
            lanNetwork.available.filter { it }.collect {
                val active = root.childStack.value.active.instance
                if (active is RootComponent.Child.ConnectionChild) {
                    active.component.onIntent(ConnectionIntent.RefreshServers)
                }
            }
        }
        lifecycleScope.launch {
            // Started while this screen is visible: Android refuses a microphone service started from the background.
            // It stops on its own when the connection ends.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                connectionRepository.connectionStatus.filter { it == ConnectionStatus.Connected }.collect {
                    startForegroundService(Intent(this@MainActivity, WearSessionService::class.java))
                }
            }
        }
        setContent { WearRoot(root) }
    }

    override fun onDestroy() {
        // Closing the app ends the session; leaving it with the home button keeps it (ADR 0011)
        if (isFinishing) connectionRepository.disconnect()
        lanNetwork.release()
        super.onDestroy()
    }
}
