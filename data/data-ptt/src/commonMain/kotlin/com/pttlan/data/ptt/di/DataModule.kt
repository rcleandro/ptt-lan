package com.pttlan.data.ptt.di

import com.pttlan.data.ptt.repository.ConnectionRepositoryImpl
import com.pttlan.domain.ptt.repository.ConnectionRepository
import org.koin.dsl.module

val dataModule =
    module {
        single<ConnectionRepository> { ConnectionRepositoryImpl(get(), get()) }
        single<com.pttlan.domain.ptt.repository.ChannelRepository> {
            com.pttlan.data.ptt.repository
                .ChannelRepositoryImpl(get())
        }
        single<com.pttlan.domain.ptt.repository.VoiceRepository> {
            com.pttlan.data.ptt.repository
                .VoiceRepositoryImpl(get(), get(), get(), get(), get())
        }
        includes(platformDataModule)
    }
