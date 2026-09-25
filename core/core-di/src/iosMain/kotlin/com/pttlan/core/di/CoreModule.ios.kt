package com.pttlan.core.di

import com.pttlan.core.audio.HeadsetMicRoute
import com.pttlan.core.audio.IosHeadsetMicRoute
import com.pttlan.core.common.share.FileSharer
import com.pttlan.core.common.share.IosFileSharer
import com.pttlan.core.common.storage.IosStorageInfoProvider
import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.database.DatabaseDriverFactory
import com.pttlan.core.datastore.SettingsFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module =
    module {
        single { DatabaseDriverFactory() }
        single { SettingsFactory() }
        single<StorageInfoProvider> { IosStorageInfoProvider() }
        single<HeadsetMicRoute> { IosHeadsetMicRoute() }
        single<FileSharer> { IosFileSharer() }
    }
