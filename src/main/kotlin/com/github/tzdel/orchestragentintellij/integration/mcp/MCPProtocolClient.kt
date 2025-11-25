package com.github.tzdel.orchestragentintellij.integration.mcp

import com.github.tzdel.orchestragentintellij.models.MCPRequest
import com.github.tzdel.orchestragentintellij.models.MCPResponse
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.pow

class MCPProtocolClient(private val processManager: ProcessManager) {

    private val logger = thisLogger()
    private val json = Json { ignoreUnknownKeys = true }

    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var reader: BufferedReader? = null
    private var writer: BufferedWriter? = null
    private var isConnected = false

    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!processManager.isProcessRunning()) {
                logger.warn("Cannot connect: MCP server process is not running")
                return@withContext false
            }

            inputStream = processManager.getInputStream()
            outputStream = processManager.getOutputStream()

            if (inputStream == null || outputStream == null) {
                logger.error("Cannot connect: Input or output stream is null")
                return@withContext false
            }

            reader = inputStream?.bufferedReader()
            writer = outputStream?.bufferedWriter()

            isConnected = true
            logger.info("Connected to MCP server")
            true
        } catch (e: Exception) {
            logger.error("Failed to connect to MCP server", e)
            isConnected = false
            false
        }
    }

    suspend fun connectWithRetry(maxRetries: Int = 5, initialDelayMs: Long = 1000): Boolean {
        var retryCount = 0

        while (retryCount < maxRetries) {
            logger.info("Attempting to connect to MCP server (attempt ${retryCount + 1}/$maxRetries)")

            if (connect()) {
                logger.info("Successfully connected to MCP server")
                return true
            }

            retryCount++
            if (retryCount < maxRetries) {
                val delayMs = (initialDelayMs * 2.0.pow(retryCount - 1)).toLong()
                logger.warn("Connection attempt $retryCount failed, retrying in ${delayMs}ms")
                delay(delayMs)
            }
        }

        logger.warn("Failed to connect to MCP server after $maxRetries attempts")
        return false
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            reader?.close()
            writer?.close()
            inputStream?.close()
            outputStream?.close()
            isConnected = false
            logger.info("Disconnected from MCP server")
        } catch (e: Exception) {
            logger.error("Error during disconnect", e)
        }
    }

    suspend fun sendRequest(request: MCPRequest): MCPResponse? = withContext(Dispatchers.IO) {
        if (!isConnected) {
            logger.error("Cannot send request: Not connected to MCP server")
            return@withContext null
        }

        return@withContext try {
            val requestJson = json.encodeToString(request)
            logger.debug("Sending request: $requestJson")

            writer?.write(requestJson)
            writer?.newLine()
            writer?.flush()

            val responseLine = reader?.readLine()
            if (responseLine == null) {
                logger.error("Received null response from MCP server")
                return@withContext null
            }

            logger.debug("Received response: $responseLine")
            json.decodeFromString<MCPResponse>(responseLine)
        } catch (e: Exception) {
            logger.error("Error sending request to MCP server", e)
            null
        }
    }

    fun isConnected(): Boolean = isConnected && processManager.isProcessRunning()
}
