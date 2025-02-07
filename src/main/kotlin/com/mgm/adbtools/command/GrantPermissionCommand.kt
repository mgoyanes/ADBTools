package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.isAppInstall
import com.mgm.adbtools.premission.ListItem

class GrantPermissionCommand : Command2<String, ListItem, Unit> {
    override fun execute(p: String, p2: ListItem, project: Project, device: IDevice) {
        if (device.isAppInstall(p))
            device.executeShellCommandWithTimeout("pm grant $p ${p2.name}", ShellOutputReceiver())
        else
            throw Exception("Application $p not installed")
    }
}
