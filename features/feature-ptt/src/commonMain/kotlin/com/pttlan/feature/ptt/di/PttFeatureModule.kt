package com.pttlan.feature.ptt.di

import com.arkivanov.decompose.ComponentContext
import com.pttlan.feature.ptt.PttComponent
import org.koin.dsl.module

val pttFeatureModule =
    module {
        factory { (componentContext: ComponentContext, channelId: String, userId: String) ->
            PttComponent(componentContext, channelId, userId, get(), get())
        }
    }
