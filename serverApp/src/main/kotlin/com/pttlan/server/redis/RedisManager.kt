package com.pttlan.server.redis

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.lettuce.core.pubsub.RedisPubSubListener
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection
import io.lettuce.core.pubsub.api.async.RedisPubSubAsyncCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisManager(
    private val redisUri: String = "redis://localhost:6379",
) {
    private val client: RedisClient = RedisClient.create(redisUri)
    private var connection: StatefulRedisConnection<String, String>? = null
    private var pubSubConnection: StatefulRedisPubSubConnection<String, String>? = null

    private val _pubSubFlow = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 100)
    val pubSubFlow: Flow<Pair<String, String>> = _pubSubFlow.asSharedFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    @Suppress("TooGenericExceptionCaught")
    fun start() {
        try {
            connection = client.connect()
            pubSubConnection = client.connectPubSub()
            println("RedisManager: Connected to Redis successfully.")
        } catch (e: Exception) {
            println("RedisManager: Failed to connect to Redis: ${e.message}")
        }

        pubSubConnection?.addListener(
            object : RedisPubSubListener<String, String> {
                override fun message(
                    channel: String,
                    message: String,
                ) {
                    scope.launch { _pubSubFlow.emit(Pair(channel, message)) }
                }

                override fun message(
                    pattern: String?,
                    channel: String?,
                    message: String?,
                ) {
                    // Not needed
                }

                override fun subscribed(
                    channel: String?,
                    count: Long,
                ) {
                    // Not needed
                }

                override fun psubscribed(
                    pattern: String?,
                    count: Long,
                ) {
                    // Not needed
                }

                override fun unsubscribed(
                    channel: String?,
                    count: Long,
                ) {
                    // Not needed
                }

                override fun punsubscribed(
                    pattern: String?,
                    count: Long,
                ) {
                    // Not needed
                }
            },
        )
    }

    fun getCommands(): RedisCoroutinesCommands<String, String>? = connection?.coroutines()

    fun getPubSubCommands(): RedisPubSubAsyncCommands<String, String>? = pubSubConnection?.async()

    suspend fun subscribe(vararg channels: String) {
        getPubSubCommands()?.subscribe(*channels)?.await()
    }

    suspend fun publish(
        channel: String,
        message: String,
    ) {
        getCommands()?.publish(channel, message)
    }

    fun stop() {
        connection?.close()
        pubSubConnection?.close()
        client.shutdown()
    }
}
