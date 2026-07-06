package com.pttlan.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.feature.connection.ConnectionComponent
import com.pttlan.feature.connection.ConnectionScreen
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val connectionRepository: ConnectionRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val connectionComponent = ConnectionComponent(
            componentContext = defaultComponentContext(),
            connectionRepository = connectionRepository
        )

        setContent {
            PttTheme {
                ConnectionScreen(component = connectionComponent)
            }
        }
    }
}
