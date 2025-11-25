package com.github.tzdel.orchestragentintellij.services

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertNotNull
import java.nio.file.Files
import java.nio.file.Path

class ConfigurationServiceTest {

    private lateinit var configurationService: ConfigurationService

    @BeforeEach
    fun setUp() {
        configurationService = ConfigurationService()
    }

    @Test
    fun `SHOULD return default settings WHEN no configuration exists`() {
        // arrange
        // Fresh instance

        // act
        val settings = configurationService.getSettings()

        // assert
        assertNotNull(settings)
        assertEquals("", settings.mcpServerPath)
        assertEquals("", settings.repositoryPath)
        assertTrue(settings.autoStartServer)
        assertEquals(30, settings.refreshIntervalSeconds)
        assertEquals("main", settings.baseBranch)
        assertEquals("", settings.testCommand)
    }

    @Test
    fun `SHOULD save and retrieve settings WHEN settings are updated`() {
        // arrange
        val expectedMcpServerPath = "/usr/local/bin/orchestragent"
        val expectedRepositoryPath = "/home/user/project"
        val expectedAutoStartServer = false
        val expectedRefreshInterval = 60
        val expectedBaseBranch = "develop"
        val expectedTestCommand = "make test"

        // act
        configurationService.updateSettings(
            mcpServerPath = expectedMcpServerPath,
            repositoryPath = expectedRepositoryPath,
            autoStartServer = expectedAutoStartServer,
            refreshIntervalSeconds = expectedRefreshInterval,
            baseBranch = expectedBaseBranch,
            testCommand = expectedTestCommand
        )
        val settings = configurationService.getSettings()

        // assert
        assertEquals(expectedMcpServerPath, settings.mcpServerPath)
        assertEquals(expectedRepositoryPath, settings.repositoryPath)
        assertEquals(expectedAutoStartServer, settings.autoStartServer)
        assertEquals(expectedRefreshInterval, settings.refreshIntervalSeconds)
        assertEquals(expectedBaseBranch, settings.baseBranch)
        assertEquals(expectedTestCommand, settings.testCommand)
    }

    @Test
    fun `SHOULD validate binary path WHEN path exists and is executable`() {
        // arrange
        val tempFile = Files.createTempFile("mcp-server", ".exe")
        tempFile.toFile().setExecutable(true)

        // act
        val isValid = configurationService.validateBinaryPath(tempFile)

        // assert
        assertTrue(isValid)

        // cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `SHOULD not validate binary path WHEN path does not exist`() {
        // arrange
        val nonExistentPath = Path.of("/nonexistent/path/to/binary")

        // act
        val isValid = configurationService.validateBinaryPath(nonExistentPath)

        // assert
        assertFalse(isValid)
    }

    @Test
    fun `SHOULD not validate binary path WHEN path is not executable`() {
        // arrange
        val isWindows = System.getProperty("os.name").lowercase().contains("win")

        if (isWindows) {
            // On Windows, file permissions work differently, skip this test
            // Windows doesn't have the same executable bit concept as Unix
            return
        }

        val tempFile = Files.createTempFile("mcp-server", ".txt")
        tempFile.toFile().setExecutable(false)

        // act
        val isValid = configurationService.validateBinaryPath(tempFile)

        // assert
        assertFalse(isValid)

        // cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `SHOULD resolve bundled binary path WHEN bundled binary exists`() {
        // arrange
        // Assuming bundled binary would be in plugin resources

        // act
        val bundledPath = configurationService.resolveBundledBinaryPath()

        // assert
        assertNotNull(bundledPath)
    }

    @Test
    fun `SHOULD use custom binary path WHEN custom path is configured`() {
        // arrange
        val customPath = "/custom/path/to/mcp-server"
        configurationService.updateSettings(mcpServerPath = customPath)

        // act
        val resolvedPath = configurationService.resolveServerBinaryPath()

        // assert
        assertEquals(customPath, resolvedPath)
    }

    @Test
    fun `SHOULD use bundled binary path WHEN no custom path is configured`() {
        // arrange
        configurationService.updateSettings(mcpServerPath = "")

        // act
        val resolvedPath = configurationService.resolveServerBinaryPath()

        // assert
        assertNotNull(resolvedPath)
        assertTrue(resolvedPath.isNotEmpty())
    }
}
