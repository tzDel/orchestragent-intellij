package com.github.tzdel.orchestragentintellij.integration.mcp

import com.github.tzdel.orchestragentintellij.models.MCPRequest
import com.github.tzdel.orchestragentintellij.models.MCPResponse
import io.mockk.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.extension.ExtendWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@ExtendWith(io.mockk.junit5.MockKExtension::class)
class MCPProtocolClientTest {

    private lateinit var processManager: ProcessManager
    private lateinit var mcpProtocolClient: MCPProtocolClient

    @BeforeEach
    fun setUp() {
        processManager = mockk(relaxed = true)
        mcpProtocolClient = MCPProtocolClient(processManager)
    }

    @Test
    fun `SHOULD connect successfully WHEN process is running`() = runBlocking {
        // arrange
        every { processManager.isProcessRunning() } returns true
        every { processManager.getInputStream() } returns ByteArrayInputStream(ByteArray(0))
        every { processManager.getOutputStream() } returns ByteArrayOutputStream()

        // act
        val connected = mcpProtocolClient.connect()

        // assert
        assertTrue(connected, "Should connect successfully when process is running")
    }

    @Test
    fun `SHOULD disconnect gracefully WHEN disconnect is called`() = runBlocking {
        // arrange
        every { processManager.isProcessRunning() } returns true
        every { processManager.getInputStream() } returns ByteArrayInputStream(ByteArray(0))
        every { processManager.getOutputStream() } returns ByteArrayOutputStream()
        mcpProtocolClient.connect()

        // act + assert
        assertDoesNotThrow { mcpProtocolClient.disconnect() }
    }

    @Test
    fun `SHOULD send request WHEN connected`() = runBlocking {
        // arrange
        val responseJson = """{"jsonrpc":"2.0","id":"test-1","result":{"success":true}}"""
        val inputStream = ByteArrayInputStream("${responseJson}\n".toByteArray())
        val outputStream = ByteArrayOutputStream()

        every { processManager.isProcessRunning() } returns true
        every { processManager.getInputStream() } returns inputStream
        every { processManager.getOutputStream() } returns outputStream

        mcpProtocolClient.connect()

        val request = MCPRequest(
            id = "test-1",
            method = "test_method",
            params = null
        )

        // act
        val response = mcpProtocolClient.sendRequest(request)

        // assert
        assertNotNull(response, "Response should not be null")
        assertEquals("test-1", response.id)
        assertEquals("2.0", response.jsonrpc)
    }

    @Test
    fun `SHOULD retry connection with exponential backoff WHEN connection fails`() = runBlocking {
        // arrange
        var attemptCount = 0
        every { processManager.isProcessRunning() } answers {
            attemptCount++
            attemptCount > 2
        }
        every { processManager.getInputStream() } returns ByteArrayInputStream(ByteArray(0))
        every { processManager.getOutputStream() } returns ByteArrayOutputStream()

        // act
        val connected = mcpProtocolClient.connectWithRetry(maxRetries = 3, initialDelayMs = 10)

        // assert
        assertTrue(connected, "Should connect after retries")
        assertTrue(attemptCount > 1, "Should have retried at least once")
    }

    @Test
    fun `SHOULD handle connection timeout WHEN max retries exceeded`() = runBlocking {
        // arrange
        every { processManager.isProcessRunning() } returns false

        // act
        val connected = mcpProtocolClient.connectWithRetry(maxRetries = 2, initialDelayMs = 10)

        // assert
        assertFalse(connected, "Should fail to connect after max retries")
    }

    @Test
    fun `SHOULD serialize request correctly WHEN sending request`() = runBlocking {
        // arrange
        val outputStream = ByteArrayOutputStream()
        val inputStream = ByteArrayInputStream("""{"jsonrpc":"2.0","id":"test-1","result":{}}""".toByteArray())

        every { processManager.isProcessRunning() } returns true
        every { processManager.getInputStream() } returns inputStream
        every { processManager.getOutputStream() } returns outputStream

        mcpProtocolClient.connect()

        val request = MCPRequest(
            id = "test-1",
            method = "tools/call",
            params = null
        )

        // act
        mcpProtocolClient.sendRequest(request)
        val sentData = outputStream.toString()

        // assert
        assertTrue(sentData.contains("tools/call"), "Request should contain method name")
        assertTrue(sentData.contains("test-1"), "Request should contain id")
    }
}
