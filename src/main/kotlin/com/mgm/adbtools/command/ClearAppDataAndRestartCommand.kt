package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.clearAppData
import com.mgm.adbtools.getDefaultActivityForApplication
import com.mgm.adbtools.isAppInstall
import com.mgm.adbtools.startActivity

class ClearAppDataAndRestartCommand : Command<String, Unit> {
    override fun execute(p: String, project: Project, device: IDevice) {
        if (device.isAppInstall(p)) {
            device.clearAppData(p)

            val activity = device.getDefaultActivityForApplication(p)
            if (activity.isNotEmpty()) {
                device.startActivity(activity)
            } else {
                throw Exception("No Default Activity Found")
            }

        } else
            throw Exception("Application $p not installed")
    }
}
