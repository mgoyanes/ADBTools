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
        const val TASK_PREFIX = "* Task{"
        val extractAppRegex = Regex("(A=|I=|u0\\s)([a-zA-Z0-9._]+)")
        val extractActivityRegex = Regex("(u0\\s[a-zA-Z0-9._]+/)([a-zA-Z0-9._]+)")
        val taskVisibleRegex = Regex("visible=(true|false)")
    }

    override fun execute(p: Any, project: Project, device: IDevice): List<BackStackData> {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY activities | grep -E \"\\* Task\\{|Hist  #\"", shellOutputReceiver)
        return getCurrentRunningActivitiesAboveApi11(device, shellOutputReceiver.toString())
    }

    /**
     * The raw dump interleaves each Task's `visible=true|false` line with the Hist (activity)
     * lines belonging to that same task, in top-to-bottom (most recent task first) order. Only
     * the topmost Hist entry of a visible task is actually on screen right now - everything else
     * (any task with visible=false, or a deeper Hist entry paused underneath the top one within a
     * visible task) is sitting in the back stack.
     */
    private fun getCurrentRunningActivitiesAboveApi11(device: IDevice, bulkActivitiesData: String): List<BackStackData> {
        lateinit var appPackage: String
        lateinit var activity: String
        var taskVisible = false
        var isTopOfTask = false
        val entries = mutableListOf<Pair<String, ActivityData>>()

        bulkActivitiesData.lines().forEach { rawLine ->
            val line = rawLine.trim()

            when {
                line.startsWith(TASK_PREFIX) -> {
                    taskVisible = taskVisibleRegex.find(line)?.groupValues?.get(1) == "true"
                    isTopOfTask = true
                }
                line.startsWith(HIST_PREFIX) -> {
                    appPackage = extractAppRegex.find(line)?.groups?.lastOrNull()?.value ?: EMPTY
                    activity = extractActivityRegex.find(line)?.groups?.lastOrNull()?.value
                        ?.let { activityName ->
                            when {
                                activityName.startsWith(ACTIVITY_PREFIX_DELIMITER) -> "$appPackage$activityName"
                                else -> activityName
                            }
                        }
                        ?: EMPTY

                    if (appPackage.isNotBlank()) {
                        entries += appPackage to ActivityData(
                            activity = activity,
                            isKilled = isKilled(device, activity),
                            isCurrent = taskVisible && isTopOfTask
                        )
                    }
                    isTopOfTask = false
                }
            }
        }

        return entries
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .map { (pkg, activities) -> BackStackData(pkg, activities) }
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
