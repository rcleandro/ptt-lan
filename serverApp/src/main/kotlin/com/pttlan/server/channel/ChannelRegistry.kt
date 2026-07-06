package com.pttlan.server.channel

import java.util.concurrent.ConcurrentHashMap

class ChannelRegistry {
    private val channels = ConcurrentHashMap<String, PttChannel>()

    fun getOrCreateChannel(channelId: String): PttChannel {
        return channels.getOrPut(channelId) { PttChannel(channelId) }
    }
    
    fun getChannel(channelId: String): PttChannel? {
        return channels[channelId]
    }

    fun removeChannelIfEmpty(channelId: String) {
        // Not implemented for Phase 4 MVP, but could clean up empty channels
    }
}
