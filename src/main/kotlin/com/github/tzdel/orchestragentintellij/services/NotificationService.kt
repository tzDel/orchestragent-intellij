package com.github.tzdel.orchestragentintellij.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project

@Service(Service.Level.APP)
class NotificationService {

    private val logger = thisLogger()
    private val notificationGroup = "orchestragent.notifications"

    fun notifyInfo(project: Project?, title: String, message: String) {
        notify(project, title, message, NotificationType.INFORMATION)
        logger.info("$title: $message")
    }

    fun notifyWarning(project: Project?, title: String, message: String) {
        notify(project, title, message, NotificationType.WARNING)
        logger.warn("$title: $message")
    }

    fun notifyError(project: Project?, title: String, message: String) {
        notify(project, title, message, NotificationType.ERROR)
        logger.error("$title: $message")
    }

    private fun notify(project: Project?, title: String, message: String, type: NotificationType) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup(notificationGroup)
                .createNotification(title, message, type)
                .notify(project)
        } catch (e: Exception) {
            logger.error("Failed to show notification: $title - $message", e)
        }
    }
}
