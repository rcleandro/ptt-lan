package com.pttlan.core.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarHost
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.feature.channellist.ChannelListScreen
import com.pttlan.feature.connection.ConnectionScreen
import com.pttlan.feature.history.HistoryScreen
import com.pttlan.feature.ptt.PttScreen
import com.pttlan.feature.settings.SettingsScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(component: RootComponent) {
    val childStack by component.childStack.subscribeAsState()
    val isCacheEnabled by component.isCacheEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarType by remember { mutableStateOf(PttSnackbarType.Generic) }

    LaunchedEffect(Unit) {
        SnackbarController.events.collect { event ->
            snackbarType = event.type
            snackbarHostState.showSnackbar(event.message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(getScreenTitle(childStack.active.instance)) },
                navigationIcon = {
                    if (childStack.backStack.isNotEmpty()) {
                        IconButton(onClick = component::goBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            val currentChild = childStack.active.instance

            val showConfigFab =
                currentChild is RootComponent.Child.ConnectionChild ||
                    currentChild is RootComponent.Child.ChannelListChild

            val showHistoryFab =
                currentChild is RootComponent.Child.PttChild && isCacheEnabled

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AnimatedVisibility(
                    visible = showHistoryFab,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    FloatingActionButton(
                        onClick = {
                            val activeChild = component.childStack.value.active.instance
                            if (activeChild is RootComponent.Child.PttChild) {
                                activeChild.component.onIntent(com.pttlan.feature.ptt.PttIntent.GoToHistory)
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Histórico de Áudios",
                        )
                    }
                }

                AnimatedVisibility(
                    visible = showConfigFab,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut(),
                ) {
                    FloatingActionButton(onClick = component::navigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                        )
                    }
                }
            }
        },
        snackbarHost = { PttSnackbarHost(snackbarHostState, snackbarType) },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Children(
                stack = childStack,
                animation = stackAnimation(slide()),
            ) { child ->
                when (val instance = child.instance) {
                    is RootComponent.Child.ConnectionChild -> ConnectionScreen(instance.component)
                    is RootComponent.Child.ChannelListChild -> ChannelListScreen(instance.component)
                    is RootComponent.Child.PttChild -> PttScreen(instance.component)
                    is RootComponent.Child.HistoryChild -> HistoryScreen(instance.component)
                    is RootComponent.Child.SettingsChild -> SettingsScreen(instance.component)
                }
            }
        }
    }
}

private fun getScreenTitle(child: RootComponent.Child): String =
    when (child) {
        is RootComponent.Child.ConnectionChild -> "Conectar"
        is RootComponent.Child.ChannelListChild -> "Canais"
        is RootComponent.Child.PttChild -> "Rádio (PTT)"
        is RootComponent.Child.HistoryChild -> "Histórico"
        is RootComponent.Child.SettingsChild -> "Configurações"
    }
