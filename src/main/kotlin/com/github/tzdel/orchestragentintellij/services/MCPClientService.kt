package com.github.tzdel.orchestragentintellij.services

import com.github.tzdel.orchestragentintellij.integration.mcp.MCPProtocolClient
import com.github.tzdel.orchestragentintellij.integration.mcp.ProcessManager
import com.github.tzdel.orchestragentintellij.models.MCPRequest
import com.github.tzdel.orchestragentintellij.models.MCPResponse
import com.github.tzdel.orchestragentintellij.models.MCPToolCallParams
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Service(Service.Level.APP)
class MCPClientService {

    private val logger = thisLogger()
    private val json = Json { ignoreUnknownKeys = true }

    private var processManager: ProcessManager
    private var protocolClient: MCPProtocolClient
    private var configurationService: ConfigurationService
    private var notificationService: NotificationService

    constructor() {
        // Default constructor for IntelliJ service instantiation
        this.processManager = ProcessManager()
        this.protocolClient = MCPProtocolClient(processManager)
        this.configurationService = com.intellij.openapi.components.service()
        this.notificationService = com.intellij.openapi.components.service()
    }

    constructor(
        processManager: ProcessManager,
        protocolClient: MCPProtocolClient,
        configurationService: ConfigurationService,
        notificationService: NotificationService
    ) {
        // Test constructor with mocked dependencies
        this.processManager = processManager
        this.protocolClient = protocolClient
        this.configurationService = configurationService
        this.notificationService = notificationService
    }

    suspend fun startServerAndConnect(): Boolean {
        return try {
            val binaryPath = configurationService.resolveServerBinaryPath()
            val repositoryPath = configurationService.getSettings().repositoryPath

            logger.info("Starting MCP server: $binaryPath")

            val arguments = if (repositoryPath.isNotBlank()) {
                listOf("--repository", repositoryPath)
            } else {
                emptyList()
            }

            val processStarted = processManager.startProcess(binaryPath, arguments)
            if (!processStarted) {
                logger.warn("Failed to start MCP server process")
                notificationService.notifyError(null, "MCP Server Error", "Failed to start MCP server process")
                return false
            }

            logger.info("Connecting to MCP server...")
            val connected = protocolClient.connectWithRetry(maxRetries = 5, initialDelayMs = 1000)

            if (connected) {
                logger.info("Successfully connected to MCP server")
                notificationService.notifyInfo(null, "MCP Server", "Connected to MCP server")
            } else {
                logger.warn("Failed to connect to MCP server")
                notificationService.notifyError(null, "MCP Server Error", "Failed to connect to MCP server")
            }

            connected
        } catch (e: Exception) {
            logger.error("Error starting MCP server and connecting", e)
            notificationService.notifyError(null, "MCP Server Error", "Error: ${e.message}")
            false
        }
    }

    suspend fun callTool(toolName: String, arguments: Map<String, String>): MCPResponse? {
        return try {
            val requestId = UUID.randomUUID().toString()
            val toolCallParams = MCPToolCallParams(name = toolName, arguments = arguments)
            val paramsJson = json.encodeToString(toolCallParams)

            val request = MCPRequest(
                id = requestId,
                method = "tools/call",
                params = json.parseToJsonElement(paramsJson)
            )

            logger.info("Calling MCP tool: $toolName with arguments: $arguments")
            val response = protocolClient.sendRequest(request)

            if (response?.error != null) {
                logger.warn("MCP tool call error: ${response.error.message}")
                notificationService.notifyError(
                    null,
                    "MCP Tool Error",
                    "Tool $toolName failed: ${response.error.message}"
                )
            } else {
                logger.info("MCP tool call successful: $toolName")
            }

            response
        } catch (e: Exception) {
            logger.error("Error calling MCP tool: $toolName", e)
            notificationService.notifyError(null, "MCP Tool Error", "Error calling tool: ${e.message}")
            null
        }
    }

    suspend fun shutdown() {
        try {
            logger.info("Shutting down MCP client service")
            protocolClient.disconnect()
            processManager.stopProcess()
            logger.info("MCP client service shutdown complete")
        } catch (e: Exception) {
            logger.error("Error during shutdown", e)
        }
    }

    fun isConnected(): Boolean {
        return protocolClient.isConnected()
    }
}
