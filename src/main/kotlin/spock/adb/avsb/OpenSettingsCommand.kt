package spock.adb.avsb

import com.android.ddmlib.IDevice
import spock.adb.ShellOutputReceiver
import spock.adb.command.NoInputCommand
import spock.adb.executeShellCommandWithTimeout

class OpenSettingsCommand : NoInputCommand<String> {

    override fun execute(device: IDevice): String {

        device.executeShellCommandWithTimeout("am start -a android.settings.SETTINGS", ShellOutputReceiver())

        return "Opened Settings"
    }
}
