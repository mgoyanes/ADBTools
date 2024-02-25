package spock.adb

import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver
import java.util.concurrent.TimeUnit
import spock.adb.command.DontKeepActivitiesState
import spock.adb.command.EnableDarkModeState
import spock.adb.command.Network
import spock.adb.command.NetworkState
import spock.adb.command.ShowLayoutBoundsState
import spock.adb.command.ShowTapsState

fun IDevice.forceKillApp(applicationID: String?, seconds: Long) {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommand("am force-stop $applicationID", shellOutputReceiver, seconds, TimeUnit.SECONDS)
}

fun IDevice.isAppInstall(applicationID: String?): Boolean {
    val shellOutputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("pm list packages $applicationID", shellOutputReceiver)
    return shellOutputReceiver.toString().isNotEmpty()
}

fun IDevice.startActivity(activity: String) {
    executeShellCommandWithTimeout("am start -n $activity", ShellOutputReceiver())
}

fun IDevice.clearAppData(applicationID: String?, seconds: Long) {
    executeShellCommand("pm clear $applicationID", ShellOutputReceiver(), seconds, TimeUnit.SECONDS)
}

fun IDevice.getDefaultActivityForApplication(packageName: String?): String {
    val outputReceiver = ShellOutputReceiver()
    if (isNougatOrAbove())
        executeShellCommandWithTimeout(
            "cmd package resolve-activity --brief $packageName | tail -n 1",
            outputReceiver
        )
    else {
        executeShellCommandWithTimeout(
            "pm dump $packageName | grep -B 10 category\\.LAUNCHER | grep -o '[^ ]*/[^ ]*' | tail -n 1",
            outputReceiver,
        )
    }
    return outputReceiver.toString()
}

fun IDevice.isMarshmallow() = this.version.apiLevel >= 23
fun IDevice.isNougatOrAbove() = this.version.apiLevel >= 24

fun IDevice.areDontKeepActivitiesEnabled(): DontKeepActivitiesState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get global always_finish_activities", outputReceiver)

    return DontKeepActivitiesState.getState(outputReceiver.toString())
}

fun IDevice.areShowTapsEnabled(): ShowTapsState {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("settings get system show_touches", outputReceiver)

    return ShowTapsState.getState(outputReceiver.toString())
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

fun IDevice.getApiVersion(): Int? {
    val outputReceiver = ShellOutputReceiver()
    executeShellCommandWithTimeout("getprop ro.build.version.release", outputReceiver)

    return outputReceiver.toString().toIntOrNull()
}

fun IDevice.executeShellCommandWithTimeout(command: String, receiver: IShellOutputReceiver) {
    executeShellCommand(
        command,
        receiver,
        MAX_TIME_TO_OUTPUT_RESPONSE,
        TimeUnit.SECONDS,
    )
}
