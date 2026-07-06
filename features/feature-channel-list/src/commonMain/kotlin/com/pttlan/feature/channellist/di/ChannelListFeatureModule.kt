package com.pttlan.feature.channellist.di

import com.arkivanov.decompose.ComponentContext
import com.pttlan.feature.channellist.ChannelListComponent
import org.koin.dsl.module

val channelListFeatureModule = module {
    factory { (componentContext: ComponentContext) -> 
        ChannelListComponent(componentContext, get()) 
    }
}
