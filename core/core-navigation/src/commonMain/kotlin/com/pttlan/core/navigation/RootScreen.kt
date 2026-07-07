package com.pttlan.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.pttlan.feature.channellist.ChannelListScreen
import com.pttlan.feature.connection.ConnectionScreen
import com.pttlan.feature.ptt.PttScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScreen(component: RootComponent) {
    val childStack by component.childStack.subscribeAsState()

    Scaffold(
        topBar = {
            if (childStack.backStack.isNotEmpty()) {
                TopAppBar(
                    title = { Text(getScreenTitle(childStack.active.instance)) },
                    navigationIcon = {
                        IconButton(onClick = component::goBack) {
                            Text("<")
                        }
                    }
                )
            }
        }
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
                }
            }
        }
    }
}

private fun getScreenTitle(child: RootComponent.Child): String {
    return when (child) {
        is RootComponent.Child.ConnectionChild -> "Conectar"
        is RootComponent.Child.ChannelListChild -> "Canais"
        is RootComponent.Child.PttChild -> "Rádio (PTT)"
    }
}
