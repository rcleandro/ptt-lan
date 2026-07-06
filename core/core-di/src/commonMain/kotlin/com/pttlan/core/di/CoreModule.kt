package com.pttlan.core.di

import com.pttlan.core.database.DatabaseDriverFactory
import com.pttlan.core.database.PttDatabase
import com.pttlan.core.datastore.SettingsFactory
import com.pttlan.core.network.createHttpClient
import org.koin.core.module.Module
import org.koin.dsl.module
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit

val coreModule = module {
    single { createHttpClient() }
    single { com.pttlan.core.network.discovery.ServerDiscoveryService() }
    
    // Database
    single { get<DatabaseDriverFactory>().createDriver() }
    single { PttDatabase(get()) }
    
    // Datastore
    single { get<SettingsFactory>().createSettings() }
    
    // Telemetry
    single<Logger> { Logger(loggerConfigInit()) }
}

expect val platformModule: Module

fun appModules() = listOf(
    coreModule, 
    platformModule,
    com.pttlan.domain.ptt.di.domainModule,
    com.pttlan.data.ptt.di.dataModule,
    com.pttlan.feature.connection.di.connectionFeatureModule
)
