package com.pttlan.core.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import com.pttlan.core.database.DatabaseDriverFactory
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsFactory
import com.pttlan.core.network.createHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module

val coreModule =
    module {
        single { createHttpClient() }
        single {
            com.pttlan.core.network
                .PttWebSocketClient(get())
        }
        single {
            com.pttlan.core.network.discovery
                .ServerDiscoveryService()
        }

        single { get<DatabaseDriverFactory>().createDriver() }
        single { PttDatabase(get()) }

        // Audio
        single {
            com.pttlan.core.audio
                .createAudioRecorder()
        }
        single {
            com.pttlan.core.audio
                .createAudioPlayer()
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
        com.pttlan.domain.ptt.di.domainModule,
        com.pttlan.data.ptt.di.dataModule,
        com.pttlan.feature.connection.di.connectionFeatureModule,
        com.pttlan.feature.channellist.di.channelListFeatureModule,
        com.pttlan.feature.ptt.di.pttFeatureModule,
        com.pttlan.feature.settings.di.settingsFeatureModule,
    )
