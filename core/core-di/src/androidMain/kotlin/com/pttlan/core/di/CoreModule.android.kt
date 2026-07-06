package com.pttlan.core.di

import com.pttlan.core.database.DatabaseDriverFactory
import com.pttlan.core.datastore.SettingsFactory
import org.koin.dsl.module
import org.koin.core.module.Module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(get()) }
    single { SettingsFactory(get()) }
}
