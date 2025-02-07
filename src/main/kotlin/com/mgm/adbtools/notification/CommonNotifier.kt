package com.mgm.adbtools.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.mgm.adbtools.EMPTY

class CommonNotifier {
    companion object {
        fun showNotifier(
            project: Project, title: String = "ADBTools",
            content: String = EMPTY,
            type: NotificationType = NotificationType.INFORMATION
        ) {
            val notification = Notification("ADBTools", title, content, type)
            notification.notify(project)
        }
    }
}
