package com.pttlan.data.ptt.di

import com.pttlan.core.common.storage.StorageInfoProvider
import com.pttlan.core.network.PttWebSocketClient
import com.pttlan.core.network.protocol.ControlMessage
import com.pttlan.data.ptt.repository.ChannelRepositoryImpl
import com.pttlan.data.ptt.repository.ChannelSessionRepositoryImpl
import com.pttlan.data.ptt.repository.ConnectionRepositoryImpl
import com.pttlan.data.ptt.repository.HeadsetMicSession
import com.pttlan.data.ptt.repository.HistoryRecorder
import com.pttlan.data.ptt.repository.HistoryRepositoryImpl
import com.pttlan.data.ptt.repository.VoiceRepositoryImpl
import com.pttlan.domain.ptt.repository.ChannelRepository
import com.pttlan.domain.ptt.repository.ChannelSessionRepository
import com.pttlan.domain.ptt.repository.ConnectionRepository
import com.pttlan.domain.ptt.repository.HistoryRepository
import com.pttlan.domain.ptt.repository.VoiceRepository
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import org.koin.core.qualifier.named
import org.koin.dsl.module

val dataModule =
    module {
        single<ConnectionRepository> { ConnectionRepositoryImpl(get(), get(), get()) }
        single(createdAtStart = true) { HeadsetMicSession(get<ConnectionRepository>().connectionStatus, get(), get()) }
        single<ChannelSessionRepository> { ChannelSessionRepositoryImpl(get()) }
        single<ChannelRepository>(createdAtStart = true) {
            ChannelRepositoryImpl(get(), get())
        }
        single {
            HistoryRecorder(
                database = get(),
                settings = get(),
                storageInfoProvider = get<StorageInfoProvider>(),
            )
        }
        single<VoiceRepository> {
            VoiceRepositoryImpl(
                audioRecorder = get(),
                audioPlayer = get(),
                webSocketClient = get(),
                pcmCodec = get(named("pcm")),
                opusCodec = get(named("opus")),
                settings = get(),
                recorder = get(),
            )
        }
        single<HistoryRepository> {
            HistoryRepositoryImpl(
                audioPlayer = get(),
                database = get(),
                settings = get(),
                storageInfoProvider = get<StorageInfoProvider>(),
                liveSpeaking =
                    get<PttWebSocketClient>()
                        .controlMessages
                        .filterIsInstance<ControlMessage.SpeakerChanged>()
                        .map { message -> message.isSpeaking },
            )
        }
    }
