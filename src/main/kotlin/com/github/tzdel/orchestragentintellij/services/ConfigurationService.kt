package com.github.tzdel.orchestragentintellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

private const val BINARY_DIRECTORY = "bin"
private const val WINDOWS_PLATFORM = "windows-x64"
private const val WINDOWS_BINARY_NAME = "orchestragent.exe"

@Service(Service.Level.PROJECT)
class ConfigurationService(private val project: Project) {

    fun getBinaryPath(): String = "$BINARY_DIRECTORY/$WINDOWS_PLATFORM/$WINDOWS_BINARY_NAME"

    fun getRepositoryPath(): String? = project.basePath

    fun getRefreshInterval(): Int = 30
}
