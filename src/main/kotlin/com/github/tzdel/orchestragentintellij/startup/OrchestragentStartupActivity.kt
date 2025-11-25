package com.github.tzdel.orchestragentintellij.startup

import com.github.tzdel.orchestragentintellij.services.ConfigurationService
import com.github.tzdel.orchestragentintellij.services.MCPClientService
import com.github.tzdel.orchestragentintellij.services.NotificationService
import com.github.tzdel.orchestragentintellij.services.SessionManagerService
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.nio.file.Paths

class OrchestragentStartupActivity : ProjectActivity {

    private val logger = thisLogger()

    private var configurationService: ConfigurationService
    private var mcpClientService: MCPClientService
    private var sessionManagerService: SessionManagerService
    private var notificationService: NotificationService

    constructor() {
        // Default constructor for IntelliJ instantiation
        this.configurationService = service()
        this.notificationService = service()
        this.mcpClientService = service()
        this.sessionManagerService = service()
    }

    constructor(
        configurationService: ConfigurationService,
        mcpClientService: MCPClientService,
        sessionManagerService: SessionManagerService,
        notificationService: NotificationService
    ) {
        // Test constructor with mocked dependencies
        this.configurationService = configurationService
        this.mcpClientService = mcpClientService
        this.sessionManagerService = sessionManagerService
        this.notificationService = notificationService
    }

    override suspend fun execute(project: Project) {
        logger.info("Orchestragent plugin initialization started")

        val settings = configurationService.getSettings()

        if (!settings.autoStartServer) {
            logger.info("Auto-start is disabled, skipping MCP server startup")
            return
        }

        try {
            val binaryPath = configurationService.resolveServerBinaryPath()
            val binaryPathObj = Paths.get(binaryPath)

            if (!configurationService.validateBinaryPath(binaryPathObj)) {
                logger.warn("MCP server binary not found or not executable: $binaryPath")
                notificationService.notifyWarning(
                    project,
                    "MCP Server Configuration Required",
                    "MCP server binary not found. Please configure the server path in settings."
                )
                return
            }

            logger.info("Starting MCP server and establishing connection")
            val connected = mcpClientService.startServerAndConnect()

            if (!connected) {
                logger.error("Failed to connect to MCP server")
                return
            }

            logger.info("Initializing session list from MCP server")
            sessionManagerService.initializeFromServer()

            logger.info("Orchestragent plugin initialization completed successfully")
        } catch (e: Exception) {
            logger.error("Error during plugin initialization", e)
            notificationService.notifyError(
                project,
                "Orchestragent Initialization Error",
                "Failed to initialize plugin: ${e.message}"
            )
        }
    }
}

typealias PluginStartupActivity = OrchestragentStartupActivity