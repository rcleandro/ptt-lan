package com.pttlan.feature.ptt

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun PttScreen(component: PttComponent) {
    val state by component.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        component.effects.collect { effect ->
            when (effect) {
                is PttEffect.NavigateBack -> {
                    // Handled by Decompose router
                }
                is PttEffect.ShowFloorDenied -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(effect.reason)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Canal: #${state.channelId}",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            val buttonColor = if (state.isTransmitting) Color.Red 
                else if (state.floorBlocked) Color.Gray 
                else MaterialTheme.colorScheme.primary

            Surface(
                shape = CircleShape,
                color = buttonColor,
                modifier = Modifier
                    .size(200.dp)
                    .pointerInput(state.floorBlocked) {
                        detectTapGestures(
                            onPress = {
                                component.onIntent(PttIntent.PressPtt)
                                tryAwaitRelease()
                                component.onIntent(PttIntent.ReleasePtt)
                            }
                        )
                    }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.isTransmitting) "FALANDO" else if (state.floorBlocked) "OCUPADO" else "PTT",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (state.currentSpeakerId != null) {
                Text(
                    text = "${state.currentSpeakerId} está falando...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            } else {
                Text(
                    text = "Canal livre",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            Button(onClick = { component.onIntent(PttIntent.LeaveChannel) }) {
                Text("Sair do Canal")
            }
        }
    }
}
