package spock.adb.command

import com.android.ddmlib.IDevice
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout

class OpenDeveloperOptionsCommand : NoInputCommand<String> {

    override fun execute(device: IDevice): String {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("am start -a com.android.settings.APPLICATION_DEVELOPMENT_SETTINGS", shellOutputReceiver)

        return "Opened Developer Options"
    }
}
