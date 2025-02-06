package spock.adb.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.GET_TOP_ACTIVITY_COMMAND
import spock.adb.ShellOutputReceiver
import spock.adb.command.NoInputCommand
import spock.adb.executeShellCommandWithTimeout

class TalkbackToggleCommand : NoInputCommand<String> {

    companion object {
        private const val GET_TALKBACK_SETTING = "settings get secure enabled_accessibility_services"
        private const val TALKBACK_SETTING_VALUE = "com.google.android.marvin.talkback/com.google.android.marvin.talkback.TalkBackService"
        private const val SET_TALKBACK_SETTING = "settings put secure enabled_accessibility_services $TALKBACK_SETTING_VALUE"
        private const val RESET_TALKBACK_SETTING = "settings put secure enabled_accessibility_services null"
    }

    override fun execute(device: IDevice): String {
        val result = ShellOutputReceiver()

        device.executeShellCommandWithTimeout(GET_TALKBACK_SETTING, result)

        return when {
            result.toString().contains(TALKBACK_SETTING_VALUE, true) -> {
                device.executeShellCommandWithTimeout(RESET_TALKBACK_SETTING, result)
                "Talkback disabled"
            }

            else -> {
                device.executeShellCommandWithTimeout(SET_TALKBACK_SETTING, result)
                "Talkback enabled"
            }
        }
    }
}
