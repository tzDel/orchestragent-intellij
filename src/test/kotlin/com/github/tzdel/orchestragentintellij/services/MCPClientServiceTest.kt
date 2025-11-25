package com.github.tzdel.orchestragentintellij.services

import com.github.tzdel.orchestragentintellij.integration.mcp.MCPProtocolClient
import com.github.tzdel.orchestragentintellij.integration.mcp.ProcessManager
import com.github.tzdel.orchestragentintellij.models.MCPResponse
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.Assertions.*

@ExtendWith(MockKExtension::class)
class MCPClientServiceTest {

    @RelaxedMockK
    private lateinit var processManager: ProcessManager

    @RelaxedMockK
    private lateinit var protocolClient: MCPProtocolClient

    @RelaxedMockK
    private lateinit var configurationService: ConfigurationService

    @RelaxedMockK
    private lateinit var notificationService: NotificationService

    private lateinit var mcpClientService: MCPClientService

    @BeforeEach
    fun setUp() {
        mcpClientService = MCPClientService(
            processManager = processManager,
            protocolClient = protocolClient,
            configurationService = configurationService,
            notificationService = notificationService
        )
    }

    @Test
    fun `SHOULD start server and connect WHEN startServerAndConnect is called`() = runBlocking {
        // arrange
        val binaryPath = "/usr/bin/orchestragent"
        every { configurationService.resolveServerBinaryPath() } returns binaryPath
        coEvery { processManager.startProcess(any(), any()) } returns true
        coEvery { protocolClient.connectWithRetry(any(), any()) } returns true

        // act
        val result = mcpClientService.startServerAndConnect()

        // assert
        assertTrue(result, "Should successfully start server and connect")
        coVerify { processManager.startProcess(binaryPath, any()) }
        coVerify { protocolClient.connectWithRetry(any(), any()) }
    }

    @Test
    fun `SHOULD return false WHEN process fails to start`() = runBlocking {
        // arrange
        val binaryPath = "/usr/bin/orchestragent"
        every { configurationService.resolveServerBinaryPath() } returns binaryPath
        coEvery { processManager.startProcess(any(), any()) } returns false

        // act
        val result = mcpClientService.startServerAndConnect()

        // assert
        assertFalse(result, "Should return false when process fails to start")
        coVerify(exactly = 0) { protocolClient.connectWithRetry(any(), any()) }
    }

    @Test
    fun `SHOULD return false WHEN connection fails`() = runBlocking {
        // arrange
        val binaryPath = "/usr/bin/orchestragent"
        every { configurationService.resolveServerBinaryPath() } returns binaryPath
        coEvery { processManager.startProcess(any(), any()) } returns true
        coEvery { protocolClient.connectWithRetry(any(), any()) } returns false

        // act
        val result = mcpClientService.startServerAndConnect()

        // assert
        assertFalse(result, "Should return false when connection fails")
    }

    @Test
    fun `SHOULD invoke tool successfully WHEN callTool is called`() = runBlocking {
        // arrange
        val toolName = "get_sessions"
        val arguments = mapOf("key" to "value")
        val mockResponse = MCPResponse(
            jsonrpc = "2.0",
            id = "test-id",
            result = buildJsonObject { put("success", true) }
        )

        coEvery { protocolClient.sendRequest(any()) } returns mockResponse

        // act
        val result = mcpClientService.callTool(toolName, arguments)

        // assert
        assertNotNull(result, "Result should not be null")
        coVerify { protocolClient.sendRequest(match { it.method == "tools/call" }) }
    }

    @Test
    fun `SHOULD disconnect and stop server WHEN shutdown is called`() {
        // act & assert
        assertDoesNotThrow {
            runBlocking {
                mcpClientService.shutdown()
            }
        }
    }

    @Test
    fun `SHOULD check connection status WHEN isConnected is called`() {
        // arrange
        every { protocolClient.isConnected() } returns true

        // act
        val isConnected = mcpClientService.isConnected()

        // assert
        assertTrue(isConnected, "Should report connected status")
        verify { protocolClient.isConnected() }
    }

    @Test
    fun `SHOULD handle error response WHEN MCP server returns error`() = runBlocking {
        // arrange
        val toolName = "invalid_tool"
        val mockErrorResponse = MCPResponse(
            jsonrpc = "2.0",
            id = "test-id",
            error = com.github.tzdel.orchestragentintellij.models.MCPError(
                code = -32601,
                message = "Method not found"
            )
        )

        coEvery { protocolClient.sendRequest(any()) } returns mockErrorResponse

        // act
        val result = mcpClientService.callTool(toolName, emptyMap())

        // assert
        assertNotNull(result, "Result should not be null even with error")
        assertNotNull(result?.error, "Error should be present in response")
    }
}
