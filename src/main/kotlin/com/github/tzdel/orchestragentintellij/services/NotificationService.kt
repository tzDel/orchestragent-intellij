package com.github.tzdel.orchestragentintellij.services

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

private const val NOTIFICATION_GROUP_ID = "orchestragent"

@Service(Service.Level.PROJECT)
class NotificationService(private val project: Project) {

    fun notifySuccess(title: String, message: String) = notify(title, message, NotificationType.INFORMATION)

    fun notifyWarning(title: String, message: String) = notify(title, message, NotificationType.WARNING)

    fun notifyError(title: String, message: String) = notify(title, message, NotificationType.ERROR)

    private fun notify(title: String, message: String, notificationType: NotificationType) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(title, message, notificationType)

        Notifications.Bus.notify(notification, project)
    }
}
