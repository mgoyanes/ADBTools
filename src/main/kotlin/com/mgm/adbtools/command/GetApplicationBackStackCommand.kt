package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import org.jetbrains.kotlin.idea.base.util.substringAfterLastOrNull
import com.mgm.adbtools.ACTIVITY_PREFIX_DELIMITER
import com.mgm.adbtools.DUMPSYS_ACTIVITY
import com.mgm.adbtools.LINE_SEPARATOR
import com.mgm.adbtools.ONE
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.models.ActivityData
import com.mgm.adbtools.models.FragmentData

class GetApplicationBackStackCommand : ListCommand<String, List<ActivityData>> {

    companion object {
        private const val UNKNOWN_COMMAND = "Unknown command"
        val extractActivityRegex = Regex("(ACTIVITY\\s)([a-zA-Z.]+/[a-zA-Z.]+)")
    }

    override fun execute(list: List<String>, device: IDevice): List<ActivityData> {
        var shellOutputReceiver: ShellOutputReceiver
        var activityData: List<String>
        var topActivity: String
        var fullActivityName: String
        val activitiesData = mutableListOf<ActivityData>()
        var currentFragmentsFromLog: List<FragmentData>

        list.forEach loop@{ identifier ->
            shellOutputReceiver = ShellOutputReceiver()
            device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY $identifier", shellOutputReceiver)

            if (shellOutputReceiver.toString().startsWith(UNKNOWN_COMMAND)) {
                return@loop
            }

            val activityLog = shellOutputReceiver.toString().lines()
            activityLog.forEach innerLoop@{ line ->
                topActivity = getActivityName(line) ?: return@innerLoop
                fullActivityName = when {
                    topActivity.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$identifier$topActivity"
                    else -> topActivity
                }

                shellOutputReceiver = ShellOutputReceiver()
                device.executeShellCommandWithTimeout(
                    "$DUMPSYS_ACTIVITY $fullActivityName",
                    shellOutputReceiver,
                )

                activityData = shellOutputReceiver.toString().lines()
                currentFragmentsFromLog = GetFragmentsCommand().getCurrentFragmentsFromLog(activityData.joinToString(LINE_SEPARATOR))

                val activity = fullActivityName.substringAfterLastOrNull(ACTIVITY_PREFIX_DELIMITER)
                activitiesData.add(
                    ActivityData(
                        activity = fullActivityName,
                        activityStackPosition = getStackPosition(device, identifier, activity),
                        isKilled = isKilled(device, activity),
                        fragment = currentFragmentsFromLog
                    )
                )
            }

            return activitiesData
        }

        return activitiesData
    }

    private fun getActivityName(bulkAppData: String) = extractActivityRegex.find(bulkAppData)?.groups?.lastOrNull()?.value

    private fun getStackPosition(device: IDevice, identifier: String, activity: String?): Int {
        activity ?: return -ONE
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

    private fun isKilled(device: IDevice, activity: String?): Boolean {
        activity ?: return true
        val isKilledRegex = Regex(".*pid=(\\d+)")
        val shellOutputReceiver = ShellOutputReceiver()

        device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY $activity | grep ACTIVITY", shellOutputReceiver)

        return shellOutputReceiver
            .toString()
            .let { pidString -> isKilledRegex.find(pidString)?.groups?.lastOrNull()?.value == null }
    }
}
