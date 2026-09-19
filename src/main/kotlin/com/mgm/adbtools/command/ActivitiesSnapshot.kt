package com.mgm.adbtools.command

import com.mgm.adbtools.ACTIVITY_PREFIX_DELIMITER
import com.mgm.adbtools.EMPTY

data class ActivityHistEntry(
    val appPackage: String,
    val activity: String,
    val histPosition: Int,
    val isCurrent: Boolean,
    val hasLiveProcess: Boolean,
    val rawLine: String
)

/**
 * Parses the raw output of `dumpsys activity activities` (unfiltered - no on-device grep) into
 * one entry per `Hist #N` line, including whether each has a live process.
 *
 * A live process shows up as an `app=ProcessRecord{...}` line before the next Hist/Task boundary
 * in the same dump. This avoids the separate `dumpsys activity <component>` round trip per
 * activity that GetBackStackCommand/GetApplicationBackStackCommand used to make to determine
 * kill state - that per-component query was measured (real device, Pixel 4a) at ~5s per call
 * regardless of whether the target activity was actually alive or killed, while this bulk dump
 * consistently takes well under 100ms even unfiltered.
 */
object ActivitiesSnapshot {
    private const val TASK_PREFIX = "* Task{"
    private const val HIST_PREFIX = "* Hist  #"
    private const val PROCESS_MARKER = "app=ProcessRecord"
    private val extractAppRegex = Regex("(A=|I=|u0\\s)([a-zA-Z0-9._]+)")
    private val extractActivityRegex = Regex("(u0\\s[a-zA-Z0-9._]+/)([a-zA-Z0-9._]+)")
    private val taskVisibleRegex = Regex("visible=(true|false)")
    private val histPositionRegex = Regex("Hist\\s+#(\\d+)")

    fun parse(dumpsysActivitiesOutput: String): List<ActivityHistEntry> {
        val entries = mutableListOf<ActivityHistEntry>()
        var taskVisible = false
        var isTopOfTask = false

        var pending: PendingEntry? = null

        fun flushPending() {
            pending?.let { entries += it.toEntry() }
            pending = null
        }

        dumpsysActivitiesOutput.lines().forEach { rawLine ->
            val line = rawLine.trim()

            when {
                line.startsWith(TASK_PREFIX) -> {
                    flushPending()
                    taskVisible = taskVisibleRegex.find(line)?.groupValues?.get(1) == "true"
                    isTopOfTask = true
                }
                line.startsWith(HIST_PREFIX) -> {
                    flushPending()
                    val appPackage = extractAppRegex.find(line)?.groups?.lastOrNull()?.value ?: EMPTY
                    val activity = extractActivityRegex.find(line)?.groups?.lastOrNull()?.value
                        ?.let { activityName ->
                            if (activityName.startsWith(ACTIVITY_PREFIX_DELIMITER)) "$appPackage$activityName" else activityName
                        }
                        ?: EMPTY
                    val position = histPositionRegex.find(line)?.groupValues?.get(1)?.toIntOrNull() ?: -1

                    if (appPackage.isNotBlank()) {
                        pending = PendingEntry(
                            appPackage = appPackage,
                            activity = activity,
                            histPosition = position,
                            isCurrent = taskVisible && isTopOfTask,
                            rawLine = line
                        )
                    }
                    isTopOfTask = false
                }
                line.contains(PROCESS_MARKER) -> {
                    pending?.hasLiveProcess = true
                }
            }
        }
        flushPending()

        return entries
    }

    private class PendingEntry(
        val appPackage: String,
        val activity: String,
        val histPosition: Int,
        val isCurrent: Boolean,
        val rawLine: String,
        var hasLiveProcess: Boolean = false
    ) {
        fun toEntry() = ActivityHistEntry(appPackage, activity, histPosition, isCurrent, hasLiveProcess, rawLine)
    }
}
