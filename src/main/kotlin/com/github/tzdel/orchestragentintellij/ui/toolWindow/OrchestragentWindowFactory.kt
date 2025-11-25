package com.github.tzdel.orchestragentintellij.ui.toolWindow

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.github.tzdel.orchestragentintellij.OrchestragentBundle
import com.github.tzdel.orchestragentintellij.services.OrchestragentService
import javax.swing.JButton

class OrchestragentWindowFactory : ToolWindowFactory {

    init {
        thisLogger().debug("Initializing OrchestragentWindowFactory.kt.")
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = OrchestragentBundle.message("toolWindow.stripeTitle")
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val orchestragentWindow = OrchestragentWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(orchestragentWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class OrchestragentWindow(toolWindow: ToolWindow) {
        private val service = toolWindow.project.service<OrchestragentService>()

        fun getContent() = JBPanel<JBPanel<*>>().apply {
            val label = JBLabel(OrchestragentBundle.message("randomLabel", "?"))

            add(label)
            add(JButton(OrchestragentBundle.message("shuffle")).apply {
                addActionListener {
                    label.text = OrchestragentBundle.message("randomLabel", service.getRandomNumber())
                }
            })
        }
    }
}
