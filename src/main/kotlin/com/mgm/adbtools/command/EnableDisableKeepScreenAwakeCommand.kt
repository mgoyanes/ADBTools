package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.isKeepScreenAwakeEnabled
import com.mgm.adbtools.executeShellCommandWithTimeout

class EnableDisableKeepScreenAwakeCommand : Command<Any, String> {

    override fun execute(p: Any, project: Project, device: IDevice): String {

        return when (device.isKeepScreenAwakeEnabled()) {
            KeepScreenAwakeState.DISABLED -> {
                setKeepScreenAwakeState(device, KeepScreenAwakeState.ENABLED)
                "Enabled keep screen awake"
            }
            KeepScreenAwakeState.ENABLED -> {
                setKeepScreenAwakeState(device, KeepScreenAwakeState.DISABLED)
                "Disabled keep screen awake"
            }
        }
    }

    private fun setKeepScreenAwakeState(device: IDevice, state: KeepScreenAwakeState) {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("settings put global stay_on_while_plugged_in ${state.state}", shellOutputReceiver)
    }
}

enum class KeepScreenAwakeState(val state: String) {
    ENABLED("7"),
    DISABLED("0");

    companion object {
        fun getState(value: String) = if (value.trim().let { it.isEmpty() || it == "0" }) DISABLED else ENABLED
    }
}
