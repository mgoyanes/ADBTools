package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.clearAppData
import com.mgm.adbtools.getDefaultActivityForApplication
import com.mgm.adbtools.isAppInstall
import com.mgm.adbtools.startActivity

class ClearAppDataAndRestartCommand : Command<String, List<String>> {
    override fun execute(p: String, project: Project, device: IDevice): List<String> {
        if (!device.isAppInstall(p)) throw Exception("Application $p not installed")
        device.clearAppData(p)
        return device.getDefaultActivityForApplication(p).ifEmpty { throw Exception("No Launcher Activity Found") }
    }
}
