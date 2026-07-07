package com.pttlan.domain.ptt.di

import com.pttlan.domain.ptt.usecase.JoinChannelUseCase
import com.pttlan.domain.ptt.usecase.LeaveChannelUseCase
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
    }
