package com.pttlan.feature.history.di

import com.arkivanov.decompose.ComponentContext
import com.pttlan.feature.history.HistoryComponent
import org.koin.dsl.module

val historyFeatureModule =
    module {
        factory { (componentContext: ComponentContext, channelId: String, onBackClicked: () -> Unit) ->
            HistoryComponent(
                componentContext = componentContext,
                channelId = channelId,
                voiceRepository = get(),
                onBackClicked = onBackClicked,
            )
        }
    }
