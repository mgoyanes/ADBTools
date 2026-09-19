package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.DUMPSYS_ACTIVITY
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout
import com.mgm.adbtools.models.ActivityData
import com.mgm.adbtools.models.BackStackData

class GetBackStackCommand : Command<Any, List<BackStackData>> {

    /**
     * Single round trip: fetch the full (unfiltered) system-wide activities dump and derive both
     * the back stack structure and each activity's kill state from it locally - see
     * ActivitiesSnapshot for why the on-device grep filter and the old per-activity
     * `dumpsys activity <activity>` kill-state probe were removed.
     */
    override fun execute(p: Any, project: Project, device: IDevice): List<BackStackData> {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY activities", shellOutputReceiver)

        return ActivitiesSnapshot.parse(shellOutputReceiver.toString())
            .groupBy(keySelector = { it.appPackage }, valueTransform = { entry ->
                ActivityData(
                    activity = entry.activity,
                    isKilled = !entry.hasLiveProcess,
                    isCurrent = entry.isCurrent
                )
            })
            .map { (pkg, activities) -> BackStackData(pkg, activities) }
    }
}
