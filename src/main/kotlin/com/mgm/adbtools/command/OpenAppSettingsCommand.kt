package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout

class OpenAppSettingsCommand : Command<String, String> {

    override fun execute(p: String, project: Project, device: IDevice): String {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("am start -a android.settings.APPLICATION_DETAILS_SETTINGS package:$p", shellOutputReceiver)

        return "Opened App Settings"
    }
}
