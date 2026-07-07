package com.pttlan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ConnectionRepository
import org.koin.android.ext.android.inject
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.VoiceRepository
import com.pttlan.core.navigation.RootComponent
import com.pttlan.core.navigation.RootScreen

class MainActivity : ComponentActivity() {


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            // Permission denied, handle if needed
        }
    }

    private lateinit var rootComponent: RootComponent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        val componentContext = defaultComponentContext()
        rootComponent = RootComponent(
            componentContext = componentContext,
        )
        
        val settings: com.russhwolf.settings.Settings by inject()
        val alwaysListening = settings.getBoolean("always_listening", true)
        
        if (alwaysListening) {
            val intent = android.content.Intent(this, PttForegroundService::class.java)
            startForegroundService(intent)
        }

        setContent {
            PttTheme {
                RootScreen(component = rootComponent)
            }
        }
    }

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
