package com.github.tzdel.orchestragentintellij.infrastructure.mcp

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ProcessManagerTest {

    private val processManager = ProcessManager()

    @AfterEach
    fun cleanup() {
        processManager.stopProcess()
    }

    @Test
    fun `startProcess SHOULD spawn process WHEN valid binary path provided`() {
        // arrange
        // Using 'cmd' on Windows which can be started without arguments
        val binaryPath = getBinaryPath()
        val repositoryPath = System.getProperty("user.dir")

        // act
        val process = processManager.startProcess(binaryPath, repositoryPath)

        // assert
        assertTrue(process.isAlive, "Process should be alive after starting")

        // cleanup
        process.destroy()
    }

    @Test
    fun `startProcess SHOULD throw exception WHEN binary not found`() {
        // arrange
        val invalidBinaryPath = "nonexistent-binary-that-does-not-exist-12345"
        val repositoryPath = System.getProperty("user.dir")

        // act & assert
        assertThrows<ProcessStartException> {
            processManager.startProcess(invalidBinaryPath, repositoryPath)
        }
    }

    @Test
    fun `isProcessAlive SHOULD return true WHEN process running`() {
        // arrange
        val binaryPath = getBinaryPath()
        val repositoryPath = System.getProperty("user.dir")
        processManager.startProcess(binaryPath, repositoryPath)

        // act
        val isAlive = processManager.isProcessAlive()

        // assert
        val expectedIsAlive = true
        assertEquals(expectedIsAlive, isAlive, "Process should be alive")
    }

    @Test
    fun `isProcessAlive SHOULD return false WHEN process terminated`() {
        // arrange
        val binaryPath = getBinaryPath()
        val repositoryPath = System.getProperty("user.dir")
        val process = processManager.startProcess(binaryPath, repositoryPath)
        process.destroy()
        process.waitFor() // Wait for process to fully terminate

        // act
        val isAlive = processManager.isProcessAlive()

        // assert
        val expectedIsAlive = false
        assertEquals(expectedIsAlive, isAlive, "Process should not be alive after termination")
    }

    @Test
    fun `stopProcess SHOULD terminate process WHEN called`() {
        // arrange
        val binaryPath = getBinaryPath()
        val repositoryPath = System.getProperty("user.dir")
        processManager.startProcess(binaryPath, repositoryPath)

        // act
        processManager.stopProcess()

        // assert
        val expectedIsAlive = false
        val actualIsAlive = processManager.isProcessAlive()
        assertEquals(expectedIsAlive, actualIsAlive, "Process should be terminated after stopProcess()")
    }

    @Test
    fun `getOutputStream SHOULD return valid stream WHEN process running`() {
        // arrange
        val binaryPath = getBinaryPath()
        val repositoryPath = System.getProperty("user.dir")
        processManager.startProcess(binaryPath, repositoryPath)

        // act
        val outputStream = processManager.getOutputStream()

        // assert
        assertNotNull(outputStream, "Output stream should not be null when process is running")
    }

    @Test
    fun `getErrorStream SHOULD return valid stream WHEN process running`() {
        // arrange
        val binaryPath = getBinaryPath()
        val repositoryPath = System.getProperty("user.dir")
        processManager.startProcess(binaryPath, repositoryPath)

        // act
        val errorStream = processManager.getErrorStream()

        // assert
        assertNotNull(errorStream, "Error stream should not be null when process is running")
    }

    private fun getBinaryPath(): String = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "cmd"
    } else {
        "cat"
    }
}
