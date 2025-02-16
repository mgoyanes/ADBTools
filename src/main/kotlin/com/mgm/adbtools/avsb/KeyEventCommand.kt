package com.mgm.adbtools.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.EMPTY
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.command.Command
import com.mgm.adbtools.executeShellCommandWithTimeout

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
        const val FOCUS = 61
        const val DONE = 66
        const val EXIT = 67
        const val DELETE = 67
        const val SEARCH = 84
        const val EPG = 172
        const val ALL_APPS = 284

        const val DEFAULT_INPUT_TIMEOUT = 200L
    }

    override fun execute(p: Int, project: Project, device: IDevice): String {
        return inputCommand(p, project, device)
    }

    override fun execute(p: Int, project: Project, device: IDevice, timeout: Long): String {
        return inputCommand(p, project, device, timeout)
    }

    private fun inputCommand(p: Int, project: Project, device: IDevice, timeout: Long = DEFAULT_INPUT_TIMEOUT): String {
        device.executeShellCommandWithTimeout("input keyevent $p", ShellOutputReceiver(), timeout)

        return when (p) {
            EPG -> "Show EPG"
            BACK -> "Back pressed"
            EXIT -> "Exit pressed"
            HOME -> "Show Home"
            ALL_APPS -> "Show Apps"
            POWER -> "Power On/Off"
            SEARCH -> "Search pressed"
            else -> "Input key event $p"
        }
    }
}
