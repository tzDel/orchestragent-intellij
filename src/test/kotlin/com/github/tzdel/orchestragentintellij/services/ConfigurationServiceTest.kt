package com.github.tzdel.orchestragentintellij.services

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ConfigurationServiceTest : BasePlatformTestCase() {

    private lateinit var configurationService: ConfigurationService

    override fun setUp() {
        super.setUp()
        configurationService = project.service<ConfigurationService>()
    }

    fun `test getBinaryPath SHOULD return windows binary path`() {
        // arrange
        val expectedBinaryPath = "bin/windows-x64/orchestragent.exe"

        // act
        val actualBinaryPath = configurationService.getBinaryPath()

        // assert
        assertEquals(expectedBinaryPath, actualBinaryPath)
    }

    fun `test getRepositoryPath SHOULD return project base path`() {
        // arrange
        val expectedRepositoryPath = project.basePath

        // act
        val actualRepositoryPath = configurationService.getRepositoryPath()

        // assert
        assertEquals(expectedRepositoryPath, actualRepositoryPath)
    }

    fun `test getRefreshInterval SHOULD return 30 seconds`() {
        // arrange
        val expectedRefreshInterval = 30

        // act
        val actualRefreshInterval = configurationService.getRefreshInterval()

        // assert
        assertEquals(expectedRefreshInterval, actualRefreshInterval)
    }
}
