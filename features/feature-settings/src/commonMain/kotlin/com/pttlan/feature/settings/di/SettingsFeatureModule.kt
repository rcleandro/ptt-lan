package com.pttlan.feature.settings.di

import com.arkivanov.decompose.ComponentContext
import com.pttlan.feature.settings.SettingsComponent
import org.koin.dsl.module

val settingsFeatureModule =
    module {
        factory { (componentContext: ComponentContext) ->
            SettingsComponent(componentContext, get())
        }
    }
