package com.pttlan.feature.connection.di

import com.arkivanov.decompose.ComponentContext
import com.pttlan.feature.connection.ConnectionComponent
import org.koin.dsl.module

val connectionFeatureModule =
    module {
        factory { (componentContext: ComponentContext) ->
            ConnectionComponent(
                componentContext = componentContext,
                observeConnectionStatusUseCase = get(),
                discoverServersUseCase = get(),
                connectToServerUseCase = get(),
            )
        }
    }
