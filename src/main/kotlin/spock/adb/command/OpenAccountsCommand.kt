package spock.adb.command

import com.android.ddmlib.IDevice
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout

class OpenAccountsCommand : NoInputCommand<String> {

    override fun execute(device: IDevice): String {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("am start -a android.settings.SYNC_SETTINGS", shellOutputReceiver)

        return "Opened Accounts"
    }
}
