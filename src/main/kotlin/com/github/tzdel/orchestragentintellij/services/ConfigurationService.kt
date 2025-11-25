package com.github.tzdel.orchestragentintellij.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@State(
    name = "OrchestrAgentSettings",
    storages = [Storage("orchestragent.xml")]
)
@Service(Service.Level.APP)
class ConfigurationService : PersistentStateComponent<PluginSettings> {

    private val logger = thisLogger()
    private var settings: PluginSettings = PluginSettings()

    override fun getState(): PluginSettings {
        return settings
    }

    override fun loadState(state: PluginSettings) {
        this.settings = state
    }

    fun getSettings(): PluginSettings {
        return settings.copy()
    }

    fun updateSettings(
        mcpServerPath: String = settings.mcpServerPath,
        repositoryPath: String = settings.repositoryPath,
        autoStartServer: Boolean = settings.autoStartServer,
        refreshIntervalSeconds: Int = settings.refreshIntervalSeconds,
        baseBranch: String = settings.baseBranch,
        testCommand: String = settings.testCommand
    ) {
        settings = PluginSettings(
            mcpServerPath = mcpServerPath,
            repositoryPath = repositoryPath,
            autoStartServer = autoStartServer,
            refreshIntervalSeconds = refreshIntervalSeconds,
            baseBranch = baseBranch,
            testCommand = testCommand
        )
        logger.info("Configuration updated: $settings")
    }

    fun validateBinaryPath(binaryPath: Path): Boolean {
        if (!Files.exists(binaryPath)) {
            logger.warn("Binary path does not exist: $binaryPath")
            return false
        }

        if (!Files.isRegularFile(binaryPath)) {
            logger.warn("Binary path is not a regular file: $binaryPath")
            return false
        }

        if (!Files.isExecutable(binaryPath)) {
            logger.warn("Binary path is not executable: $binaryPath")
            return false
        }

        return true
    }

    fun resolveBundledBinaryPath(): String {
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()

        val platform = when {
            os.contains("win") -> "windows-x64"
            os.contains("mac") -> if (arch.contains("aarch64") || arch.contains("arm")) "macos-arm64" else "macos-x64"
            os.contains("nix") || os.contains("nux") || os.contains("aix") -> "linux-x64"
            else -> {
                logger.warn("Unknown platform: $os $arch, defaulting to linux-x64")
                "linux-x64"
            }
        }

        val binaryName = if (os.contains("win")) "orchestragent.exe" else "orchestragent"
        val resourcePath = "/bin/$platform/$binaryName"

        logger.info("Resolved bundled binary path: $resourcePath for platform: $platform")
        return resourcePath
    }

    fun resolveServerBinaryPath(): String {
        val customPath = settings.mcpServerPath
        return if (customPath.isNotBlank()) {
            logger.info("Using custom MCP server path: $customPath")
            customPath
        } else {
            val bundledPath = resolveBundledBinaryPath()
            logger.info("Using bundled MCP server path: $bundledPath")
            bundledPath
        }
    }
}

data class PluginSettings(
    var mcpServerPath: String = "",
    var repositoryPath: String = "",
    var autoStartServer: Boolean = true,
    var refreshIntervalSeconds: Int = 30,
    var baseBranch: String = "main",
    var testCommand: String = ""
)
