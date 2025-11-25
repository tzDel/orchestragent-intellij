package com.github.tzdel.orchestragentintellij.integration.mcp

import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

class ProcessManager {

    private val logger = thisLogger()
    private var process: Process? = null

    suspend fun startProcess(binaryPath: String, arguments: List<String>): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            if (process?.isAlive == true) {
                logger.warn("Process is already running, stopping existing process")
                stopProcess()
            }

            val commandList = mutableListOf(binaryPath)
            commandList.addAll(arguments)

            logger.info("Starting MCP server process: ${commandList.joinToString(" ")}")

            val processBuilder = ProcessBuilder(commandList)
                .redirectErrorStream(false)

            process = processBuilder.start()
            logger.info("MCP server process started successfully")
            true
        } catch (e: Exception) {
            logger.warn("Failed to start MCP server process: ${e.message}")
            false
        }
    }

    suspend fun stopProcess() = withContext(Dispatchers.IO) {
        try {
            process?.let { proc ->
                if (proc.isAlive) {
                    logger.info("Stopping MCP server process")
                    proc.destroy()

                    val terminated = proc.waitFor()
                    logger.info("MCP server process stopped with exit code: $terminated")
                } else {
                    logger.info("MCP server process is not running")
                }
            }
            process = null
        } catch (e: Exception) {
            logger.error("Error stopping MCP server process", e)
            process = null
        }
    }

    fun isProcessRunning(): Boolean {
        return process?.isAlive ?: false
    }

    fun getOutputStream(): OutputStream? {
        return process?.outputStream
    }

    fun getInputStream(): InputStream? {
        return process?.inputStream
    }

    fun getErrorStream(): InputStream? {
        return process?.errorStream
    }
}
