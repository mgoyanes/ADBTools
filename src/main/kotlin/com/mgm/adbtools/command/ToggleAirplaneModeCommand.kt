package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.getAirplaneModeState
import java.util.Locale

class ToggleAirplaneModeCommand : NoInputCommand<String> {

    override fun execute(project: Project, device: IDevice): String {

        return when (device.getAirplaneModeState()) {
            AirplaneModeState.DISABLED -> {
                setAirplaneModeState(device, AirplaneModeState.ENABLED)
                "Enabled Airplane Mode"
            }

            AirplaneModeState.ENABLED -> {
                setAirplaneModeState(device, AirplaneModeState.DISABLED)
                "Disabled Airplane Mode"
            }
        }
    }

    private fun setAirplaneModeState(device: IDevice, airplaneModeState: AirplaneModeState) {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("cmd connectivity airplane-mode ${airplaneModeState.state}", shellOutputReceiver)
    }
}

enum class AirplaneModeState(val state: String) {
    ENABLED("enable"),
    DISABLED("disable");

    companion object {
        private val map = entries.associateBy { state -> state.name.lowercase(Locale.getDefault()) }
        fun getState(value: String) = map[value] ?: DISABLED
    }
}
