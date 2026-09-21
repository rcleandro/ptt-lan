package com.pttlan.wear

import android.app.Application
import com.pttlan.core.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

/** Same graph as the phone, without the room host: the watch is only a client (ADR 0011). */
class WearApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@WearApplication)
            modules(appModules() + module { single { LanNetwork(androidContext()) } })
        }
    }
}
