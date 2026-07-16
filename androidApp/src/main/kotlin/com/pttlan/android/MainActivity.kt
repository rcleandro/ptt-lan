package com.pttlan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.retainedComponent
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.core.designsystem.theme.AppTheme
import com.pttlan.domain.ptt.repository.ConnectionRepository
import org.koin.android.ext.android.inject
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen
import com.pttlan.domain.ptt.repository.ConnectionStatus
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.pttlan.domain.ptt.repository.ChannelSessionRepository
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.coroutines.getIntFlow
import com.russhwolf.settings.coroutines.getBooleanFlow
import com.russhwolf.settings.ExperimentalSettingsApi

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
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
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

        lifecycleScope.launch {
            combine(
                connectionRepository.connectionStatus,
                channelSessionRepository.activeSessionChannelId,
                (settings as ObservableSettings).getBooleanFlow("always_listening", true)
            ) { status, activeChannel, alwaysListening ->
                Triple(status, activeChannel, alwaysListening)
            }.collect { (status, activeChannel, alwaysListening) ->
                val intent = android.content.Intent(this@MainActivity, PttForegroundService::class.java)
                if (status == ConnectionStatus.Connected && activeChannel != null && alwaysListening) {
                    startForegroundService(intent)
                } else if (status == ConnectionStatus.Disconnected || activeChannel == null || !alwaysListening) {
                    stopService(intent)
                }
            }
        }

        setContent {
            val appThemeInt by (settings as ObservableSettings).getIntFlow("app_theme", 0).collectAsState(initial = settings.getInt("app_theme", 0))
            val appTheme = AppTheme.entries.getOrElse(appThemeInt) { AppTheme.SYSTEM }

            PttTheme(appTheme = appTheme) {
                RootScreen(component = rootComponent)
            }
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
