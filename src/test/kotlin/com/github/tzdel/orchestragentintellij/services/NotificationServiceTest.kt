package com.github.tzdel.orchestragentintellij.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class NotificationServiceTest : BasePlatformTestCase() {

    private lateinit var notificationService: NotificationService

    override fun setUp() {
        super.setUp()
        notificationService = project.service<NotificationService>()
    }

    fun `test notifySuccess SHOULD display success notification WHEN called with title and message`() {
        // arrange
        val expectedTitle = "Success Title"
        val expectedMessage = "Success message content"
        val capture = captureNotifications()

        // act
        notificationService.notifySuccess(expectedTitle, expectedMessage)

        // assert
        assertNotNull("Notification should be sent", capture.captured)
        assertEquals(expectedTitle, capture.captured?.title)
        assertEquals(expectedMessage, capture.captured?.content)
        assertEquals(NotificationType.INFORMATION, capture.captured?.type)
        assertEquals("orchestragent", capture.captured?.groupId)
    }

    fun `test notifyWarning SHOULD display warning notification WHEN called with title and message`() {
        // arrange
        val expectedTitle = "Warning Title"
        val expectedMessage = "Warning message content"
        val capture = captureNotifications()

        // act
        notificationService.notifyWarning(expectedTitle, expectedMessage)

        // assert
        assertNotNull("Notification should be sent", capture.captured)
        assertEquals(expectedTitle, capture.captured?.title)
        assertEquals(expectedMessage, capture.captured?.content)
        assertEquals(NotificationType.WARNING, capture.captured?.type)
        assertEquals("orchestragent", capture.captured?.groupId)
    }

    fun `test notifyError SHOULD display error notification WHEN called with title and message`() {
        // arrange
        val expectedTitle = "Error Title"
        val expectedMessage = "Error message content"
        val capture = captureNotifications()

        // act
        notificationService.notifyError(expectedTitle, expectedMessage)

        // assert
        assertNotNull("Notification should be sent", capture.captured)
        assertEquals(expectedTitle, capture.captured?.title)
        assertEquals(expectedMessage, capture.captured?.content)
        assertEquals(NotificationType.ERROR, capture.captured?.type)
        assertEquals("orchestragent", capture.captured?.groupId)
    }

    private class NotificationCapture {
        var captured: Notification? = null
    }

    private fun captureNotifications(): NotificationCapture {
        val capture = NotificationCapture()
        project.messageBus.connect(testRootDisposable).subscribe(
            Notifications.TOPIC,
            object : Notifications {
                override fun notify(notification: Notification) {
                    capture.captured = notification
                }
            }
        )
        return capture
    }
}
