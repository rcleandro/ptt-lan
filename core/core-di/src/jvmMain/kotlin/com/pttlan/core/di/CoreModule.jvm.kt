package com.pttlan.core.di

import com.pttlan.core.common.storage.JvmStorageInfoProvider
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.database.DatabaseDriverFactory
import com.pttlan.core.datastore.SettingsFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single { DatabaseDriverFactory() }
        single { SettingsFactory() }
        single<StorageInfoProvider> { JvmStorageInfoProvider() }
    }
