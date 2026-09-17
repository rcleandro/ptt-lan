package com.pttlan.core.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarHost
import com.pttlan.core.designsystem.components.snackbar.PttSnackbarType
import com.pttlan.core.designsystem.components.snackbar.SnackbarController
import com.pttlan.core.designsystem.theme.PttTheme
import com.pttlan.feature.channellist.ChannelListScreen
import com.pttlan.feature.connection.ConnectionScreen
import com.pttlan.feature.history.HistoryScreen
import com.pttlan.feature.ptt.PttScreen
import com.pttlan.feature.settings.SettingsScreen

/**
 * App root: applies the theme and hosts the screens. Each screen draws its own floating glass
 * controls (ADR 0006), so there is no Scaffold, top app bar or FAB here.
 */
@Composable
fun RootScreen(component: RootComponent) {
    val appTheme by component.appTheme.collectAsState()
    val reduceTransparency by component.reduceTransparency.collectAsState()

    PttTheme(appTheme = appTheme, reduceTransparency = reduceTransparency) {
        RootContent(component)
    }
}

@Composable
private fun RootContent(component: RootComponent) {
    val childStack by component.childStack.subscribeAsState()
    val isCacheEnabled by component.isCacheEnabled.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarType by remember { mutableStateOf(PttSnackbarType.Generic) }
    val openHistory = if (isCacheEnabled) component::navigateToHistory else null

    LaunchedEffect(Unit) {
        SnackbarController.events.collect { event ->
            snackbarType = event.type
            snackbarHostState.showSnackbar(event.message)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Children(
            stack = childStack,
            animation = stackAnimation(slide()),
        ) { child ->
            when (val instance = child.instance) {
                is RootComponent.Child.ConnectionChild ->
                    ConnectionScreen(
                        component = instance.component,
                        onOpenSettings = component::navigateToSettings,
                        onOpenHistory = openHistory,
                    )
                is RootComponent.Child.ChannelListChild ->
                    ChannelListScreen(
                        component = instance.component,
                        onBack = component::goBack,
                        onOpenSettings = component::navigateToSettings,
                        onOpenHistory = openHistory,
                    )
                is RootComponent.Child.PttChild ->
                    PttScreen(
                        component = instance.component,
                        onBack = component::goBack,
                        showHistory = isCacheEnabled,
                    )
                is RootComponent.Child.HistoryChild -> HistoryScreen(instance.component)
                is RootComponent.Child.SettingsChild -> SettingsScreen(instance.component, onBack = component::goBack)
            }
        }

        PttSnackbarHost(
            state = snackbarHostState,
            type = snackbarType,
            modifier = Modifier.align(Alignment.TopCenter).windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}
