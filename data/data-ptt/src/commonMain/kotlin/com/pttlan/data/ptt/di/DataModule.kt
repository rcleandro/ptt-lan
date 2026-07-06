package com.pttlan.data.ptt.di

import com.pttlan.data.ptt.repository.ConnectionRepositoryImpl
import com.pttlan.domain.ptt.repository.ConnectionRepository
import org.koin.dsl.module

val dataModule = module {
    single<ConnectionRepository> { ConnectionRepositoryImpl(get(), get()) }
}
