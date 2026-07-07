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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        val componentContext = defaultComponentContext()
        val rootComponent = RootComponent(
            componentContext = componentContext,
        )

        setContent {
            PttTheme {
                RootScreen(component = rootComponent)
            }
        }
    }
}
