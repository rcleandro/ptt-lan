package com.pttlan.shared

import com.pttlan.core.di.appModules
import org.koin.core.context.startKoin

fun initKoinIos() {
    startKoin {
        modules(appModules())
    }
}
