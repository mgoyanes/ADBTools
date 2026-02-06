package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.mgm.adbtools.DUMPSYS_ACTIVITY
import com.mgm.adbtools.ONE
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.models.ActivityData
import com.mgm.adbtools.parser.DumpsysParser

class GetApplicationBackStackCommand : ListCommand<String, List<ActivityData>> {

    companion object {
        private const val UNKNOWN_COMMAND = "Unknown command"
    }

    private val parser = DumpsysParser()

    override fun execute(list: List<String>, device: IDevice): List<ActivityData> {
        var shellOutputReceiver: ShellOutputReceiver

        list.forEach loop@{ identifier ->
            shellOutputReceiver = ShellOutputReceiver()
            device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY $identifier", shellOutputReceiver)

            if (shellOutputReceiver.toString().startsWith(UNKNOWN_COMMAND)) {
                return@loop
            }

            val backStackData = parser.parse(shellOutputReceiver.toString())

            return backStackData.activitiesList.mapIndexed { _, activityData ->
                activityData.copy(
                    activityStackPosition = getStackPosition(device, identifier, activityData.activity),
                    isKilled = isKilled(device, activityData.activity)
                )
            }
        }

        return emptyList()
    }

    private fun getStackPosition(device: IDevice, identifier: String, activity: String): Int {
        if (activity.isBlank()) return -ONE
        val positionRegex = Regex(".*Hist.*#(\\d+).*")
        val shellOutputReceiver = ShellOutputReceiver()

        device.executeShellCommandWithTimeout(
            "$DUMPSYS_ACTIVITY activities | grep -E \"Hist.*${identifier}\"",
            shellOutputReceiver,
        )

        return shellOutputReceiver
            .toString()
            .lines()
            .firstOrNull { value -> value.contains(activity) }
            ?.let { position -> positionRegex.find(position)?.groups?.lastOrNull()?.value?.toIntOrNull() ?: -ONE }
            ?: -ONE
    }

    private fun isKilled(device: IDevice, activity: String): Boolean {
        if (activity.isBlank()) return true
        val isKilledRegex = Regex(".*pid=(\\d+)")
        val shellOutputReceiver = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY $activity | grep ACTIVITY", shellOutputReceiver)

        return shellOutputReceiver
            .toString()
            .let { pidString -> isKilledRegex.find(pidString)?.groups?.lastOrNull()?.value == null }
    }
}
