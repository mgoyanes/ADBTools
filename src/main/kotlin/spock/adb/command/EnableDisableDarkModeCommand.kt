package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout
import spock.adb.isDarkModeEnabled

class EnableDisableDarkModeCommand : Command<Any, String> {

    companion object {
        private const val ENABLE_DARK_MODE = "cmd uimode night yes"
        private const val DISABLE_DARK_MODE = "cmd uimode night no"
    }


    override fun execute(p: Any, project: Project, device: IDevice): String {

        return when (device.isDarkModeEnabled()) {
            EnableDarkModeState.DISABLED -> {
                setDarkModeState(device, ENABLE_DARK_MODE)
                "Enabled Dark Mode"
            }

            EnableDarkModeState.ENABLED -> {
                setDarkModeState(device, DISABLE_DARK_MODE)
                "Disabled Dark Mode"
            }
        }
    }

    private fun setDarkModeState(device: IDevice, command: String) {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout(command, shellOutputReceiver)
    }
}

enum class EnableDarkModeState(val state: String) {
    ENABLED("Night mode: yes"),
    DISABLED("Night mode: no");

    companion object {
        fun getState(value: String) = if (value == ENABLED.state) ENABLED else DISABLED
    }
}
