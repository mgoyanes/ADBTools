package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.areShowTapsEnabled
import com.mgm.adbtools.executeShellCommandWithTimeout

class EnableDisableShowTapsCommand : Command<Any, String> {

    override fun execute(p: Any, project: Project, device: IDevice): String {

        return when (device.areShowTapsEnabled()) {
            ShowTapsState.DISABLED -> {
                setShowTapsState(device, ShowTapsState.ENABLED)
                "Enabled show taps"
            }
            ShowTapsState.ENABLED -> {
                setShowTapsState(device, ShowTapsState.DISABLED)
                "Disabled show taps"
            }
        }
    }

    private fun setShowTapsState(device: IDevice, state: ShowTapsState) {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("settings put system show_touches ${state.state}", shellOutputReceiver)
    }
}

enum class ShowTapsState(val state: String) {
    ENABLED("1"),
    DISABLED("0");

    companion object {
        private val map = values().associateBy(ShowTapsState::state)
        fun getState(value: String) = map[value] ?: DISABLED
    }
}
