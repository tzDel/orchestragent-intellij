package com.github.tzdel.orchestragentintellij.integration.mcp

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.assertNotNull
import java.nio.file.Files

class ProcessManagerTest {

    private lateinit var processManager: ProcessManager

    @BeforeEach
    fun setUp() {
        processManager = ProcessManager()
    }

    @AfterEach
    fun tearDown() {
        runBlocking {
            processManager.stopProcess()
        }
    }

    @Test
    fun `SHOULD start process WHEN valid binary path is provided`() = runBlocking {
        // arrange
        val echoCommand = if (System.getProperty("os.name").lowercase().contains("win")) {
            "cmd.exe"
        } else {
            "echo"
        }
        val args = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf("/c", "echo", "test")
        } else {
            listOf("test")
        }

        // act
        val success = processManager.startProcess(echoCommand, args)

        // assert
        assertTrue(success, "Process should start successfully")
    }

    @Test
    fun `SHOULD return false WHEN starting process with invalid binary`() = runBlocking {
        // arrange
        val invalidBinary = "/nonexistent/binary"

        // act
        val success = processManager.startProcess(invalidBinary, emptyList())

        // assert
        assertFalse(success, "Process should fail to start with invalid binary")
    }

    @Test
    fun `SHOULD report running state WHEN process is started`() = runBlocking {
        // arrange
        val sleepCommand = if (System.getProperty("os.name").lowercase().contains("win")) {
            "cmd.exe"
        } else {
            "sleep"
        }
        val args = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf("/c", "timeout", "/t", "5", "/nobreak")
        } else {
            listOf("5")
        }

        // act
        processManager.startProcess(sleepCommand, args)
        val isRunning = processManager.isProcessRunning()

        // assert
        assertTrue(isRunning, "Process should be reported as running")
    }

    @Test
    fun `SHOULD stop process WHEN stopProcess is called`() = runBlocking {
        // arrange
        val sleepCommand = if (System.getProperty("os.name").lowercase().contains("win")) {
            "cmd.exe"
        } else {
            "sleep"
        }
        val args = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf("/c", "timeout", "/t", "10", "/nobreak")
        } else {
            listOf("10")
        }

        processManager.startProcess(sleepCommand, args)
        assertTrue(processManager.isProcessRunning(), "Process should be running")

        // act
        processManager.stopProcess()

        // assert
        assertFalse(processManager.isProcessRunning(), "Process should be stopped")
    }

    @Test
    fun `SHOULD provide output stream WHEN process is started`() = runBlocking {
        // arrange
        val echoCommand = if (System.getProperty("os.name").lowercase().contains("win")) {
            "cmd.exe"
        } else {
            "echo"
        }
        val args = if (System.getProperty("os.name").lowercase().contains("win")) {
            listOf("/c", "echo", "test")
        } else {
            listOf("test")
        }

        // act
        processManager.startProcess(echoCommand, args)
        val outputStream = processManager.getOutputStream()

        // assert
        assertNotNull(outputStream, "Output stream should be available")
    }

    @Test
    fun `SHOULD provide input stream WHEN process is started`() = runBlocking {
        // arrange
        val echoCommand = if (System.getProperty("os.name").lowercase().contains("win")) {
            "cmd.exe"
        } else {
            "cat"
        }
        val args = if (System.getProperty("os.name").lowercase().contains("win")) {
            emptyList<String>()
        } else {
            emptyList<String>()
        }

        // act
        processManager.startProcess(echoCommand, args)
        val inputStream = processManager.getInputStream()

        // assert
        assertNotNull(inputStream, "Input stream should be available")
    }
}
