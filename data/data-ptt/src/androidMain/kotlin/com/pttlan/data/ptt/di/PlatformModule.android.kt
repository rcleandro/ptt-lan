package com.pttlan.data.ptt.di

import com.pttlan.data.ptt.util.AndroidLocalFileCache
import com.pttlan.data.ptt.util.LocalFileCache
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformDataModule: Module = module {
    single<LocalFileCache> { AndroidLocalFileCache(get()) }
}
