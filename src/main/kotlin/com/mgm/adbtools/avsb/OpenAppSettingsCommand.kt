package com.mgm.adbtools.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.AVSB_PACKAGE
import com.mgm.adbtools.GET_TOP_ACTIVITY_COMMAND
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.command.NoInputCommand
import com.mgm.adbtools.executeShellCommandWithTimeout

class OpenAppSettingsCommand : NoInputCommand<String> {

    override fun execute(project: Project, device: IDevice): String {
        val result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout(GET_TOP_ACTIVITY_COMMAND, result)

        return when {
            result.toString().contains("com.android.tv.settings/.device.apps.AppManagementActivity", true) -> {
                KeyEventCommand().execute(KeyEventCommand.BACK, project, device)
                "Closed App Settings"
            }

            else -> {
                device.executeShellCommandWithTimeout("am start -a android.settings.APPLICATION_DETAILS_SETTINGS package:$AVSB_PACKAGE", ShellOutputReceiver())
                "Opened App Settings"
            }
        }
    }
}
