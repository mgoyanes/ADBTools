package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ACTIVITY_PREFIX_DELIMITER
import com.mgm.adbtools.DUMPSYS_ACTIVITY
import com.mgm.adbtools.EMPTY
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.models.ActivityData
import com.mgm.adbtools.models.BackStackData

class GetBackStackCommand : Command<Any, List<BackStackData>> {

    companion object {
        const val HIST_PREFIX = "* Hist"
        val extractAppRegex = Regex("(A=|I=|u0\\s)([a-zA-Z.\\d]+)")
        val extractActivityRegex = Regex("(u0\\s[a-zA-Z.\\d]+/)([a-zA-Z.\\d]+)")
    }

    override fun execute(p: Any, project: Project, device: IDevice): List<BackStackData> {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY activities | grep Hist", shellOutputReceiver)
        return getCurrentRunningActivitiesAboveApi11(device, shellOutputReceiver.toString())
    }

    private fun getCurrentRunningActivitiesAboveApi11(device: IDevice, bulkActivitiesData: String): List<BackStackData> {
        lateinit var appPackage: String
        lateinit var activity: String

        return bulkActivitiesData
            .lines()
            .filter { line -> line.trim().startsWith(HIST_PREFIX) }
            .groupBy(
                keySelector = { line ->
                    appPackage = extractAppRegex.find(line)?.groups?.lastOrNull()?.value ?: EMPTY
                    appPackage
                },
                valueTransform = { bulkActivityData ->
                    activity = extractActivityRegex.find(bulkActivityData)?.groups?.lastOrNull()?.value
                        ?.let { activityName ->
                            when {
                                activityName.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$appPackage$activityName"
                                else -> activityName
                            }
                        }
                        ?: EMPTY

                    ActivityData(activity = activity, isKilled = isKilled(device, activity))
                }
            )
            .filter { entry -> entry.key.isNotBlank() }
            .map { activityData -> BackStackData(activityData.key, activityData.value) }
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
