package com.github.tzdel.orchestragentintellij.infrastructure.mcp

import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.ClientOptions
import io.modelcontextprotocol.kotlin.sdk.shared.Transport
import io.modelcontextprotocol.kotlin.sdk.shared.TransportSendOptions
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.JSONRPCMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MCPClientFactoryTest {

    private val processManager = ProcessManager()

    @AfterEach
    fun cleanup() {
        processManager.stopProcess()
    }

    @Test
    fun `startAndConnect SHOULD use default client info WHEN none provided`() = runBlocking {
        val capturedClients = mutableListOf<CapturingClient>()
        val factory = MCPClientFactory(
            processManager = processManager,
            dispatcher = Dispatchers.Unconfined,
            clientFactory = { info, options ->
                CapturingClient(info, options).also(capturedClients::add)
            },
            transportFactory = { NoOpTransport() },
        )

        val connectedClient = factory.startAndConnect(
            binaryPath = getBinaryPath(),
            repositoryPath = System.getProperty("user.dir") ?: ".",
        )

        val capturedClient = capturedClients.single()
        assertEquals("orchestragent-intellij", capturedClient.clientInfo.name)
        assertEquals("0.1.0", capturedClient.clientInfo.version)

        connectedClient.process.destroy()
    }

    @Test
    fun `startAndConnect SHOULD wrap failures WHEN process start fails`() {
        val repositoryPath = System.getProperty("user.dir") ?: "."
        val factory = MCPClientFactory(processManager)

        val exception = assertThrows<MCPClientInitializationException> {
            runBlocking {
                factory.startAndConnect(
                    binaryPath = "nonexistent-mcp-binary",
                    repositoryPath = repositoryPath,
                )
            }
        }

        val causeChain = generateSequence(exception as Throwable?) { it.cause }
        assertTrue(
            causeChain.any { it is ProcessStartException },
            "Expected ProcessStartException anywhere in cause chain but found ${exception.cause?.javaClass?.name}",
        )
    }

    private fun getBinaryPath(): String = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "cmd"
    } else {
        "cat"
    }

    private class CapturingClient(
        val clientInfo: Implementation,
        clientOptions: ClientOptions,
    ) : Client(clientInfo, clientOptions) {
        override suspend fun connect(transport: Transport) {
            // no-op to avoid real handshake during tests
        }
    }

    private class NoOpTransport : Transport {
        override suspend fun start() = Unit

        override suspend fun send(message: JSONRPCMessage, options: TransportSendOptions?) = Unit

        override suspend fun close() = Unit

        override fun onClose(block: () -> Unit) = Unit

        override fun onError(block: (Throwable) -> Unit) = Unit

        override fun onMessage(block: suspend (JSONRPCMessage) -> Unit) = Unit
    }
}
