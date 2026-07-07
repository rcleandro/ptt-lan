package com.pttlan.domain.ptt.di

import com.pttlan.domain.ptt.usecase.ConnectToServerUseCase
import com.pttlan.domain.ptt.usecase.CreateChannelUseCase
import com.pttlan.domain.ptt.usecase.DiscoverServersUseCase
import com.pttlan.domain.ptt.usecase.GetRecentChannelsUseCase
import com.pttlan.domain.ptt.usecase.JoinChannelUseCase
import com.pttlan.domain.ptt.usecase.JoinChannelUseCaseImpl
import com.pttlan.domain.ptt.usecase.LeaveChannelUseCase
import com.pttlan.domain.ptt.usecase.ObserveActiveChannelsUseCase
import com.pttlan.domain.ptt.usecase.ObserveConnectionStatusUseCase
import com.pttlan.domain.ptt.usecase.ObserveFloorDeniedUseCase
import com.pttlan.domain.ptt.usecase.ObserveParticipantsUseCase
import com.pttlan.domain.ptt.usecase.ObserveSpeakerUseCase
import com.pttlan.domain.ptt.usecase.StartTransmittingUseCase
import com.pttlan.domain.ptt.usecase.StopTransmittingUseCase
import org.koin.dsl.module

val domainModule =
    module {
        factory { JoinChannelUseCase(get()) }
        factory { LeaveChannelUseCase(get()) }
        factory { ObserveParticipantsUseCase(get()) }
        factory { ObserveSpeakerUseCase(get()) }
        factory { ObserveFloorDeniedUseCase(get()) }
        factory { StartTransmittingUseCase(get()) }
        factory { StopTransmittingUseCase(get()) }

        factory { ObserveConnectionStatusUseCase(get()) }
        factory { DiscoverServersUseCase(get()) }
        factory { ConnectToServerUseCase(get()) }

        factory { GetRecentChannelsUseCase(get()) }
        factory { ObserveActiveChannelsUseCase(get()) }
        factory { JoinChannelUseCaseImpl(get()) }
        factory { CreateChannelUseCase(get()) }
    }
