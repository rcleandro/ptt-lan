package com.pttlan.android

import android.app.Application
import com.pttlan.core.di.appModules
import com.pttlan.domain.ptt.repository.LocalServerHost
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.bind
import org.koin.dsl.module

class PttApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@PttApplication)
            modules(appModules() + module { single { AndroidServerHost(get()) } bind LocalServerHost::class })
        }
    }
}
