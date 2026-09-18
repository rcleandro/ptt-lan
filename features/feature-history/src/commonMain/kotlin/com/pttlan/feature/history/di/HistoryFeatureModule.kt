package com.pttlan.feature.history.di

import com.arkivanov.decompose.ComponentContext
import com.pttlan.feature.history.HistoryComponent
import org.koin.dsl.module

val historyFeatureModule =
    module {
        factory { (componentContext: ComponentContext, onBackClicked: () -> Unit) ->
            HistoryComponent(
                componentContext = componentContext,
                historyRepository = get(),
                onBackClicked = onBackClicked,
            )
        }
    }
