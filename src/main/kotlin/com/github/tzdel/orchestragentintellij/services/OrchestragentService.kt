package com.github.tzdel.orchestragentintellij.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.github.tzdel.orchestragentintellij.OrchestragentBundle

@Service(Service.Level.PROJECT)
class OrchestragentService(project: Project) {

    init {
        thisLogger().debug("Initializing OrchestragentWindowFactory.kt.")
        thisLogger().info(OrchestragentBundle.message("projectService", project.name))
    }

    fun getRandomNumber() = (1..100).random()
}
