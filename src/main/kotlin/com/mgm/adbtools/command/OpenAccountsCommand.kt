package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout

class OpenAccountsCommand : NoInputCommand<String> {

    override fun execute(device: IDevice): String {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("am start -a android.settings.SYNC_SETTINGS", shellOutputReceiver)

        return "Opened Accounts"
    }
}
