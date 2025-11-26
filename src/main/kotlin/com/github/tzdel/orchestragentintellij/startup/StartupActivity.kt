package com.github.tzdel.orchestragentintellij.startup

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class StartupActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        thisLogger().debug("Execute function of OrchestragentStartupActivity.kt triggered.")
    }
}