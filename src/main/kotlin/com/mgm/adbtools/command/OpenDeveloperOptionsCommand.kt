package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout

class OpenDeveloperOptionsCommand : NoInputCommand<String> {

    override fun execute(device: IDevice): String {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("am start -a com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS", shellOutputReceiver)

        return "Opened Developer Options"
    }
}
