package com.github.tzdel.orchestragentintellij.infrastructure.mcp

import java.io.File
import java.io.InputStream
import java.io.OutputStream

class ProcessManager {
    private var process: Process? = null

    fun startProcess(binaryPath: String, repositoryPath: String): Process {
        val processBuilder = ProcessBuilder(binaryPath)
            .directory(File(repositoryPath))
            .redirectErrorStream(false)

        return tryStartServer(processBuilder, binaryPath)
    }

    private fun tryStartServer(processBuilder: ProcessBuilder, binaryPath: String): Process {
        try {
            process = processBuilder.start()
            return process!!
        } catch (exception: Exception) {
            throw ProcessStartException(
                "Failed to start MCP server at $binaryPath: ${exception.message}",
                exception
            )
        }
    }

    fun isProcessAlive(): Boolean = process
        ?.isAlive
        ?: false

    fun stopProcess() {
        process?.destroy()
        process = null
    }

    fun getOutputStream(): OutputStream = process
        ?.outputStream
        ?: throw IllegalStateException("Process is not running")

    fun getErrorStream(): InputStream = process
        ?.errorStream
        ?: throw IllegalStateException("Process is not running")
}

class ProcessStartException(message: String, cause: Throwable) : Exception(message, cause)
