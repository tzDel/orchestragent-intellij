package com.github.tzdel.orchestragentintellij.infrastructure.mcp

import org.junit.After
import org.junit.Test
import org.junit.Assert.*

class ProcessManagerTest {

    private val processManager = ProcessManager()

    @After
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
        assertTrue("Process should be alive after starting", process.isAlive)

        // cleanup
        process.destroy()
    }

    @Test
    fun `startProcess SHOULD throw exception WHEN binary not found`() {
        // arrange
        val invalidBinaryPath = "nonexistent-binary-that-does-not-exist-12345"
        val repositoryPath = System.getProperty("user.dir")

        // act & assert
        assertThrows(ProcessStartException::class.java) {
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
        assertEquals("Process should be alive", expectedIsAlive, isAlive)
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
        assertEquals("Process should not be alive after termination", expectedIsAlive, isAlive)
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
        assertEquals("Process should be terminated after stopProcess()", expectedIsAlive, actualIsAlive)
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
        assertNotNull("Output stream should not be null when process is running", outputStream)
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
        assertNotNull("Error stream should not be null when process is running", errorStream)
    }

    private fun getBinaryPath(): String = if (System.getProperty("os.name").lowercase().contains("windows")) {
        "cmd"
    } else {
        "cat"
    }
}
