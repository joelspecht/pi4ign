package com.joelspecht.pi4ign.gateway

import com.pi4j.Pi4J
import com.pi4j.context.Context
import com.pi4j.util.Console
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GatewayHookTest {

    private lateinit var piContext: Context

    @BeforeEach
    fun setup() {
        piContext = Pi4J.newContextBuilder()
            .autoDetect()
            .build()
    }

    @AfterEach
    fun tearDown() {
        piContext.shutdown()
    }

    @Test
    fun test() {
        val console = Console()
        PrintInfo.printLoadedPlatforms(console, piContext)
        PrintInfo.printDefaultPlatform(console, piContext)
        PrintInfo.printProviders(console, piContext)
        PrintInfo.printRegistry(console, piContext)
    }

}
