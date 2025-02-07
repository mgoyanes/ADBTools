package com.mgm

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.android.sdk.AndroidSdkUtils
import com.mgm.adbtools.AdbController
import com.mgm.adbtools.AdbControllerImp
import com.mgm.adbtools.AdbToolsViewer

class AdbDrawerViewer : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val adbController: AdbController = AdbControllerImp(project, AndroidSdkUtils.getDebugBridge(project), toolWindow)
        val contentManager = toolWindow.contentManager

        with(AdbToolsViewer(project)) {
            initPlugin(adbController)
            contentManager.addContent(contentManager.factory.createContent(this, null, false))
        }
    }
}
