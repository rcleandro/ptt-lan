package com.pttlan.android

import android.os.Bundle
import co.touchlab.kermit.Logger
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.retainedComponent
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pttlan.domain.ptt.repository.ConnectionRepository
import org.koin.android.ext.android.inject
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.pttlan.domain.ptt.repository.ChannelSessionRepository
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.ExperimentalSettingsApi
import android.os.Build
import com.pttlan.core.datastore.SettingsKeys
import com.pttlan.core.datastore.SettingsDefaults

@OptIn(ExperimentalSettingsApi::class)
class MainActivity : ComponentActivity() {


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions if needed
    }

    private lateinit var rootComponent: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val ungrantedPermissions = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (ungrantedPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(ungrantedPermissions.toTypedArray())
        }

        rootComponent = retainedComponent { componentContext ->
            RootComponent(componentContext = componentContext)
        }
        
        val settings: com.russhwolf.settings.Settings by inject()
        val connectionRepository: ConnectionRepository by inject()
        val channelSessionRepository: ChannelSessionRepository by inject()
        val serverHost: AndroidServerHost by inject()

        lifecycleScope.launch {
            // Only while the activity is at least STARTED: since phase 20.1 the client reconnects on its own,
            // so this used to fire `startForegroundService` with the app in the background, which Android 12+
            // rejects with ForegroundServiceStartNotAllowedException — and a `microphone` service is refused
            // outright on 14+. `repeatOnLifecycle` re-emits the current values when the app comes back.
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    connectionRepository.connectionStatus,
                    channelSessionRepository.activeSessionChannelId,
                    (settings as ObservableSettings).getBooleanFlow(SettingsKeys.ALWAYS_LISTENING, SettingsDefaults.ALWAYS_LISTENING),
                    serverHost.isHosting,
                ) { status, activeChannel, alwaysListening, hosting ->
                    listeningServiceWanted(status, activeChannel, alwaysListening, hosting)
                }.collect { wanted ->
                    val intent = android.content.Intent(this@MainActivity, PttForegroundService::class.java)
                    when (wanted) {
                        true -> startListeningService(intent)
                        false -> stopService(intent)
                        null -> Unit
                    }
                }
            }
        }

        setContent {
            RootScreen(component = rootComponent)
        }
    }

    /**
     * Starting the service can still lose a race with the activity going to the background, and the system
     * answers that with an exception instead of ignoring it. Losing background audio is bad; crashing the app
     * because of it is worse.
     */
    private fun startListeningService(intent: android.content.Intent) {
        try {
            startForegroundService(intent)
        } catch (e: IllegalStateException) {
            Logger.withTag("android").w(e) { "Could not start the listening service from the background" }
        } catch (e: SecurityException) {
            Logger.withTag("android").w(e) { "Not allowed to start the microphone service right now" }
        }
    }

    @android.annotation.SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val keyCode = event.keyCode
        // Automotive standard media keys or custom steering wheel buttons
        if (keyCode == android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ||
            keyCode == android.view.KeyEvent.KEYCODE_MEDIA_NEXT ||
            keyCode == android.view.KeyEvent.KEYCODE_HEADSETHOOK ||
            keyCode == android.view.KeyEvent.KEYCODE_SPACE // For testing on emulator
        ) {
            val isPressed = event.action == android.view.KeyEvent.ACTION_DOWN
            // Only consume if we are in a PttScreen (returns true)
            if (::rootComponent.isInitialized && rootComponent.handlePttKey(isPressed)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}
