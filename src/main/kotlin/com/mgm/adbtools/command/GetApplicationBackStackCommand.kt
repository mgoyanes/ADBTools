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

            // One extra round trip total (not one per activity): the same system-wide dump
            // GetBackStackCommand uses, which already carries both the Hist stack position and
            // process-liveness for every activity - see ActivitiesSnapshot.
            val activitiesOutputReceiver = ShellOutputReceiver()
            device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY activities", activitiesOutputReceiver)
            val histEntries = ActivitiesSnapshot.parse(activitiesOutputReceiver.toString())

            return backStackData.activitiesList.mapIndexed { _, activityData ->
                val match = findHistEntry(histEntries, identifier, activityData.activity)
                activityData.copy(
                    activityStackPosition = match?.histPosition ?: -ONE,
                    isKilled = match?.let { !it.hasLiveProcess } ?: true
                )
            }
        }

        return emptyList()
    }

    private fun findHistEntry(histEntries: List<ActivityHistEntry>, identifier: String, activity: String): ActivityHistEntry? {
        if (activity.isBlank()) return null
        return histEntries.firstOrNull { entry -> entry.rawLine.contains(identifier) && entry.rawLine.contains(activity) }
    }
}
