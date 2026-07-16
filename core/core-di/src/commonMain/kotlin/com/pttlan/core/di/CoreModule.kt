package com.pttlan.core.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import com.pttlan.core.audio.AudioCodec
import com.pttlan.core.audio.OpusAudioCodec
import com.pttlan.core.audio.PcmPassthroughCodec
import com.pttlan.core.audio.createAudioPlayer
import com.pttlan.core.audio.createAudioRecorder
import com.pttlan.core.database.DatabaseDriverFactory
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsFactory
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.createHttpClient
import com.pttlan.core.network.discovery.ServerDiscoveryService
import com.pttlan.data.ptt.di.dataModule
import com.pttlan.domain.ptt.di.domainModule
import com.pttlan.feature.channellist.di.channelListFeatureModule
import com.pttlan.feature.connection.di.connectionFeatureModule
import com.pttlan.feature.history.di.historyFeatureModule
import com.pttlan.feature.ptt.di.pttFeatureModule
import com.pttlan.feature.settings.di.settingsFeatureModule
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val coreModule =
    module {
        single { createHttpClient() }
        single {
            PttWebSocketClient(get())
        }
        single {
            ServerDiscoveryService()
        }

        single { get<DatabaseDriverFactory>().createDriver() }
        single { PttDatabase(get()) }

        // Audio
        single {
            createAudioRecorder()
        }
        single {
            createAudioPlayer()
        }

        single<AudioCodec>(named("pcm")) {
            PcmPassthroughCodec()
        }
        single<AudioCodec>(named("opus")) {
            OpusAudioCodec()
        }

        // Datastore
        single { get<SettingsFactory>().createSettings() }
        single<Logger> { Logger(loggerConfigInit()) }
    }

expect val platformModule: Module

fun appModules() =
    listOf(
        coreModule,
        platformModule,
        domainModule,
        dataModule,
        connectionFeatureModule,
        channelListFeatureModule,
        pttFeatureModule,
        historyFeatureModule,
        settingsFeatureModule,
    )
