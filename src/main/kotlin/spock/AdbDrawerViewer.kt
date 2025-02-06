package spock

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import org.jetbrains.android.sdk.AndroidSdkUtils
import spock.adb.AdbController
import spock.adb.AdbControllerImp
import spock.adb.SpockAdbViewer

class AdbDrawerViewer : ToolWindowFactory {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val adbController: AdbController = AdbControllerImp(project, AndroidSdkUtils.getDebugBridge(project), toolWindow)
        val contentManager = toolWindow.contentManager

        with(SpockAdbViewer(project)) {
            initPlugin(adbController)
            contentManager.addContent(contentManager.factory.createContent(this, null, false))
        }
    }
}
