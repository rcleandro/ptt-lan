package com.pttlan.core.navigation

import com.pttlan.core.navigation.RootComponent.Config
import kotlin.test.Test
import kotlin.test.assertEquals

class OpeningChannelTest {
    private val list = listOf(Config.Connection, Config.ChannelList)

    @Test
    fun `opening a channel from the list stacks it on top`() {
        assertEquals(list + Config.PttScreen("Geral"), list.openingChannel(Config.PttScreen("Geral")))
    }

    @Test
    fun `picking another channel beside the open one replaces it`() {
        val stack = list + Config.PttScreen("Geral")

        assertEquals(list + Config.PttScreen("Obra"), stack.openingChannel(Config.PttScreen("Obra")))
    }

    @Test
    fun `picking the open channel again keeps the stack`() {
        val stack = list + Config.PttScreen("Geral")

        assertEquals(stack, stack.openingChannel(Config.PttScreen("Geral")))
    }
}
