package com.mgm.adbtools.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.EMPTY
import com.mgm.adbtools.MIN_TIME_TO_OUTPUT_RESPONSE
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.avsb.AppsCommand.AppAction
import com.mgm.adbtools.command.Command2
import com.mgm.adbtools.executeShellCommandWithTimeout

class AppsCommand : Command2<String, AppAction, String> {

    enum class App(val openCommand: String, val closeCommand: String, val appName: String) {
        NETFLIX(
            "am start -a com.netflix.action.NETFLIX_KEY_START -n com.netflix.ninja/.MainActivity",
            "am force-stop com.netflix.ninja",
            "Netflix"
        ),
        HBO_MAX(
            "am start -a android.intent.action.MAIN -n com.wbd.stream/com.wbd.beam.BeamActivity",
            "am force-stop com.wbd.stream",
            "HBO Max"
        ),
        DISNEY_PLUS(
            "am start -a android.intent.action.VIEW -n com.disney.disneyplus/com.bamtechmedia.dominguez.main.MainActivity",
            "am force-stop com.disney.disneyplus",
            "Disney+"
        ),
        PRIME_VIDEO(
            "am start -a android.intent.action.MAIN -n com.amazon.amazonvideo.livingroom/com.amazon.ignition.IgnitionActivity",
            "am force-stop com.amazon.amazonvideo.livingroom",
            "Prime Video"
        ),
        YOUTUBE(
            "am start -a com.google.android.youtube.tv com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.ShellActivity",
            "am force-stop com.google.android.youtube.tv",
            "YouTube"
        )
    }

    enum class AppAction {
        OPEN, CLOSE
    }

    override fun execute(p: String, p2: AppAction, project: Project, device: IDevice): String {
        val app = App.entries.firstOrNull {
            it.appName.equals(p, true)
        } ?: return EMPTY

        return when (p2) {
            AppAction.OPEN -> {
                device.executeShellCommandWithTimeout(app.openCommand, ShellOutputReceiver(), MIN_TIME_TO_OUTPUT_RESPONSE)
                "Opened ${app.appName}"
            }

            AppAction.CLOSE -> {
                device.executeShellCommandWithTimeout(app.closeCommand, ShellOutputReceiver(), MIN_TIME_TO_OUTPUT_RESPONSE)
                "Closed ${app.appName}"
            }
        }
    }
}
