package spock.adb.avsb

import com.android.ddmlib.IDevice
import spock.adb.ShellOutputReceiver
import spock.adb.command.NoInputCommand
import spock.adb.executeShellCommandWithTimeout

class OpenStatusCommand : NoInputCommand<String> {

    override fun execute(device: IDevice): String {

        device.executeShellCommandWithTimeout("am start -n com.android.tv.settings/com.android.tv.settings.about.StatusActivity", ShellOutputReceiver())

        return "Opened Status"
    }
}
