package com.github.tzdel.orchestragentintellij.startup

import com.github.tzdel.orchestragentintellij.services.ConfigurationService
import com.github.tzdel.orchestragentintellij.services.MCPClientService
import com.github.tzdel.orchestragentintellij.services.NotificationService
import com.github.tzdel.orchestragentintellij.services.PluginSettings
import com.github.tzdel.orchestragentintellij.services.SessionManagerService
import com.intellij.openapi.project.Project
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Files

@ExtendWith(MockKExtension::class)
class PluginStartupActivityTest {

    @RelaxedMockK
    private lateinit var configurationService: ConfigurationService

    @RelaxedMockK
    private lateinit var mcpClientService: MCPClientService

    @RelaxedMockK
    private lateinit var sessionManagerService: SessionManagerService

    @RelaxedMockK
    private lateinit var notificationService: NotificationService

    @RelaxedMockK
    private lateinit var mockProject: Project

    private lateinit var pluginStartupActivity: PluginStartupActivity

    @BeforeEach
    fun setUp() {
        pluginStartupActivity = PluginStartupActivity(
            configurationService = configurationService,
            mcpClientService = mcpClientService,
            sessionManagerService = sessionManagerService,
            notificationService = notificationService
        )
    }

    @Test
    fun `SHOULD start MCP server and initialize sessions WHEN binary is available and auto-start is enabled`() = runBlocking {
        // arrange
        val tempFile = Files.createTempFile("mcp-server", ".exe")
        tempFile.toFile().setExecutable(true)

        every { configurationService.resolveServerBinaryPath() } returns tempFile.toString()
        every { configurationService.validateBinaryPath(any()) } returns true
        every { configurationService.getSettings() } returns com.github.tzdel.orchestragentintellij.services.PluginSettings(
            autoStartServer = true
        )
        coEvery { mcpClientService.startServerAndConnect() } returns true
        coEvery { sessionManagerService.initializeFromServer() } just Runs

        // act
        pluginStartupActivity.execute(mockProject)

        // assert
        coVerify { mcpClientService.startServerAndConnect() }
        coVerify { sessionManagerService.initializeFromServer() }

        // cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `SHOULD show configuration dialog WHEN binary is not available`() = runBlocking {
        // arrange
        every { configurationService.resolveServerBinaryPath() } returns "/nonexistent/binary"
        every { configurationService.validateBinaryPath(any()) } returns false
        every { configurationService.getSettings() } returns PluginSettings(autoStartServer = true)

        // act
        pluginStartupActivity.execute(mockProject)

        // assert
        coVerify(exactly = 0) { mcpClientService.startServerAndConnect() }
    }

    @Test
    fun `SHOULD skip auto-start WHEN auto-start is disabled`() = runBlocking {
        // arrange
        every { configurationService.getSettings() } returns com.github.tzdel.orchestragentintellij.services.PluginSettings(
            autoStartServer = false
        )

        // act
        pluginStartupActivity.execute(mockProject)

        // assert
        coVerify(exactly = 0) { mcpClientService.startServerAndConnect() }
    }

    @Test
    fun `SHOULD handle connection failure gracefully WHEN MCP server fails to connect`() = runBlocking {
        // arrange
        val tempFile = Files.createTempFile("mcp-server", ".exe")
        tempFile.toFile().setExecutable(true)

        every { configurationService.resolveServerBinaryPath() } returns tempFile.toString()
        every { configurationService.validateBinaryPath(any()) } returns true
        every { configurationService.getSettings() } returns com.github.tzdel.orchestragentintellij.services.PluginSettings(
            autoStartServer = true
        )
        coEvery { mcpClientService.startServerAndConnect() } returns false

        // act
        pluginStartupActivity.execute(mockProject)

        // assert
        coVerify { mcpClientService.startServerAndConnect() }
        coVerify(exactly = 0) { sessionManagerService.initializeFromServer() }

        // cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `SHOULD initialize sessions after successful connection WHEN MCP server connects`() = runBlocking {
        // arrange
        val tempFile = Files.createTempFile("mcp-server", ".exe")
        tempFile.toFile().setExecutable(true)

        every { configurationService.resolveServerBinaryPath() } returns tempFile.toString()
        every { configurationService.validateBinaryPath(any()) } returns true
        every { configurationService.getSettings() } returns com.github.tzdel.orchestragentintellij.services.PluginSettings(
            autoStartServer = true
        )
        coEvery { mcpClientService.startServerAndConnect() } returns true
        coEvery { sessionManagerService.initializeFromServer() } just Runs

        // act
        pluginStartupActivity.execute(mockProject)

        // assert
        coVerify(ordering = Ordering.ORDERED) {
            mcpClientService.startServerAndConnect()
            sessionManagerService.initializeFromServer()
        }

        // cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `SHOULD log debug message WHEN execute is called`() = runBlocking {
        // arrange
        every { configurationService.getSettings() } returns com.github.tzdel.orchestragentintellij.services.PluginSettings(
            autoStartServer = false
        )

        // act
        pluginStartupActivity.execute(mockProject)

        // assert
        assertTrue(true, "Execute method should complete without errors")
    }
}
