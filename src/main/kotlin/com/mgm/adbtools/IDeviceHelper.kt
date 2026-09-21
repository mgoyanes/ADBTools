package com.mgm.adbtools

import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver
import com.mgm.adbtools.command.AirplaneModeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import com.mgm.adbtools.command.DontKeepActivitiesState
import com.mgm.adbtools.command.EnableDarkModeState
import com.mgm.adbtools.command.KeepScreenAwakeState
import com.mgm.adbtools.command.Network
import com.mgm.adbtools.command.NetworkState
import com.mgm.adbtools.command.ShowLayoutBoundsState
import com.mgm.adbtools.command.ShowTapsState
import java.util.concurrent.TimeUnit

fun IDevice.forceKillApp(applicationID: String?) {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("am force-stop $applicationID", shellOutputReceiver)
}

fun IDevice.isAppInstall(applicationID: String?): Boolean {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("pm list packages $applicationID", shellOutputReceiver)
    return shellOutputReceiver.toString().isNotEmpty()
}

fun IDevice.startActivity(activity: String) {
    executeShellCommandWithTimeout("am start -n $activity", ShellOutputReceiver())
}

fun IDevice.clearAppData(applicationID: String?) {
    executeShellCommandWithTimeout("pm clear $applicationID", ShellOutputReceiver())
}

fun IDevice.getDefaultActivityForApplication(packageName: String?): List<String> {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout(
        "cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LAUNCHER | grep $packageName",
        outputReceiver
    )
    return outputReceiver.toString()
        .lines()
        .distinct()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

fun IDevice.isMarshmallow() = this.version.isAtLeast(23)

fun IDevice.areDontKeepActivitiesEnabled(): DontKeepActivitiesState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get global always_finish_activities", outputReceiver)

    return DontKeepActivitiesState.getState(outputReceiver.toString())
}

private const val UI_DUMP_PATH = "/data/local/tmp/adbtools_window_dump.xml"

fun IDevice.dumpUiHierarchy(precedingCommand: String? = null): String {
    val dumpCommand = "uiautomator dump $UI_DUMP_PATH; cat $UI_DUMP_PATH"
    val command = if (precedingCommand != null) "$precedingCommand; $dumpCommand" else dumpCommand

    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout(command, outputReceiver)
    return outputReceiver.toString()
}

fun IDevice.pressBack() {
    executeShellCommandWithTimeout("input keyevent 4", ShellOutputReceiver())
}

fun IDevice.tapThenPressBack(x: Int, y: Int, settleDelaySeconds: Double = 0.3) {
    executeShellCommandWithTimeout("input tap $x $y; sleep $settleDelaySeconds; input keyevent 4", ShellOutputReceiver())
}

fun IDevice.scrollToBottomBlindCommand(width: Int, height: Int): String {
    val x = width / 2
    val fromY = (height * 0.94).toInt()
    val toY = (height * 0.05).toInt()
    val fling = "input swipe $x $fromY $x $toY 80"
    return "$fling; $fling; $fling; sleep 0.3"
}

fun IDevice.scrollUpCommand(width: Int, height: Int): String {
    val x = width / 2
    // The top ~25% of the screen is the fixed toolbar / "Use developer options" switch row,
    // not part of the scrollable list. The touch-down point has to start below that or the
    // gesture is captured by that non-scrolling header instead of scrolling the list.
    val fromY = (height * 0.25).toInt()
    val toY = (height * 0.90).toInt()
    return "input swipe $x $fromY $x $toY 300"
}

fun IDevice.getScreenSize(): Pair<Int, Int> {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("wm size", outputReceiver)
    val match = Regex("""(\d+)x(\d+)""").find(outputReceiver.toString())
    return match?.destructured?.let { (width, height) -> width.toInt() to height.toInt() } ?: (1080 to 1920)
}

fun IDevice.areShowTapsEnabled(): ShowTapsState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get system show_touches", outputReceiver)

    return ShowTapsState.getState(outputReceiver.toString())
}

fun IDevice.isKeepScreenAwakeEnabled(): KeepScreenAwakeState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get global stay_on_while_plugged_in", outputReceiver)

    return KeepScreenAwakeState.getState(outputReceiver.toString())
}

fun IDevice.areShowLayoutBoundsEnabled(): ShowLayoutBoundsState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("getprop debug.layout", outputReceiver)

    return ShowLayoutBoundsState.getState(outputReceiver.toString())
}

fun IDevice.isDarkModeEnabled(): EnableDarkModeState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("cmd uimode night", outputReceiver)

    return EnableDarkModeState.getState(outputReceiver.toString())
}

fun IDevice.refreshUi() {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("service call activity 1599295570", shellOutputReceiver)
}

fun IDevice.getWindowAnimatorScale(): String {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get global window_animation_scale", shellOutputReceiver)
    return shellOutputReceiver.toString()
}

fun IDevice.getTransitionAnimationScale(): String {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get global transition_animation_scale", shellOutputReceiver)
    return shellOutputReceiver.toString()
}

fun IDevice.getAnimatorDurationScale(): String {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get global animator_duration_scale", shellOutputReceiver)
    return shellOutputReceiver.toString()
}

fun IDevice.getNetworkRateLimit(): String {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get global ingress_rate_limit_bytes_per_second", shellOutputReceiver)
    return shellOutputReceiver.toString()
}

fun IDevice.isAppInForeground(applicationID: String?): Boolean {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("$DUMPSYS_ACTIVITY recents | grep 'Recent #0'", shellOutputReceiver)
    return shellOutputReceiver.toString().contains(applicationID.toString(), true)
}

fun IDevice.getNetworkState(network: Network): NetworkState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get global ${network.networkSettingIdentifier}", outputReceiver)

    return NetworkState.getState(outputReceiver.toString())
}

fun IDevice.getAirplaneModeState(): AirplaneModeState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("cmd connectivity airplane-mode", outputReceiver)

    return AirplaneModeState.getState(outputReceiver.toString())
}

fun IDevice.getFirebaseDebugApp(): String {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("getprop debug.firebase.analytics.app", outputReceiver)

    return outputReceiver.toString()
}

fun IDevice.getDMS(): String {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get secure com_vodafone_vtv_dms", shellOutputReceiver)
    return shellOutputReceiver.toString()
}

fun IDevice.executeShellCommandWithTimeout(command: String, receiver: IShellOutputReceiver, timeout: Long = MAX_TIME_TO_OUTPUT_RESPONSE, timeUnit: TimeUnit = TimeUnit.SECONDS) {
    runBlocking(Dispatchers.IO) {
        executeShellCommand(
            command,
            receiver,
            timeout,
            timeUnit,
        )
    }
}

