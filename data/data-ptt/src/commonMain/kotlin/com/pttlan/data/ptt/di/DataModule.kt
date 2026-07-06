package com.pttlan.data.ptt.di

import com.pttlan.data.ptt.repository.ChannelRepositoryImpl
import com.pttlan.data.ptt.repository.ConnectionRepositoryImpl
import com.pttlan.data.ptt.repository.VoiceRepositoryImpl
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.VoiceRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dataModule =
    module {
        single<ConnectionRepository> { ConnectionRepositoryImpl(get(), get()) }
        single<ChannelRepository> {
            ChannelRepositoryImpl(get())
        }
        single<VoiceRepository> {
            VoiceRepositoryImpl(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(named("pcm")),
                get(named("opus")),
                get(),
            )
        }
        includes(platformDataModule)
    }
