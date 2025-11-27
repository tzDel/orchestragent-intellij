package com.github.tzdel.orchestragentintellij.infrastructure.mcp

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MCPClientFactoryTest {

    private val factory = MCPClientFactory(ProcessManager())

    @Test
    fun `buildClientInfo SHOULD return provided name and version`() {
        val name = "custom-client"
        val version = "9.9.9"

        val clientInfo = factory.buildClientInfo(name, version)

        assertEquals(name, clientInfo.name)
        assertEquals(version, clientInfo.version)
    }

    @Test
    fun `startAndConnect SHOULD wrap failures WHEN process start fails`() {
        val repositoryPath = System.getProperty("user.dir") ?: "."

        val exception = assertThrows(MCPClientInitializationException::class.java) {
            runBlocking {
                factory.startAndConnect(
                    binaryPath = "nonexistent-mcp-binary",
                    repositoryPath = repositoryPath,
                )
            }
        }

        val causeChain = generateSequence(exception as Throwable?) { it.cause }
        assertTrue(
            "Expected ProcessStartException anywhere in cause chain but found ${exception.cause?.javaClass?.name}",
            causeChain.any { it is ProcessStartException }
        )
    }
}
