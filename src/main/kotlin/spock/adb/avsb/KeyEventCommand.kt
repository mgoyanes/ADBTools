package spock.adb.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.EMPTY
import spock.adb.ShellOutputReceiver
import spock.adb.ZERO
import spock.adb.command.Command
import spock.adb.executeShellCommandWithTimeout

class KeyEventCommand : Command<Int, String> {

    companion object {
        const val HOME = 3
        const val BACK = 4
        const val FOUR = 11
        const val TWO = 9
        const val DPAD_UP = 19
        const val DPAD_DOWN = 20
        const val DPAD_RIGHT = 22
        const val OK = 23
        const val POWER = 26
        const val EXIT = 67
        const val SEARCH = 84
        const val EPG = 172
        const val ALL_APPS = 284
    }

    override fun execute(p: Int, project: Project, device: IDevice): String {

        device.executeShellCommandWithTimeout("input keyevent $p", ShellOutputReceiver(), ZERO.toLong())

        return when (p) {
            EPG -> "Show EPG"
            BACK -> "Back pressed"
            EXIT -> "Exit pressed"
            HOME -> "Show Home"
            ALL_APPS -> "Show Apps"
            POWER -> "Power On/Off"
            SEARCH -> "Search pressed"
            else -> EMPTY
        }
    }
}
