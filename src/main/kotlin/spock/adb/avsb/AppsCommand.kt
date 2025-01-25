package spock.adb.avsb

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.MIN_TIME_TO_OUTPUT_RESPONSE
import spock.adb.ShellOutputReceiver
import spock.adb.command.Command
import spock.adb.executeShellCommandWithTimeout

class AppsCommand : Command<AppsCommand.App, String> {

    enum class App(val command: String, val appName: String) {
        NETFLIX("am start -a com.netflix.action.NETFLIX_KEY_START -n com.netflix.ninja/.MainActivity", "Netflix"),
        HBO_MAX("am start -a android.intent.action.MAIN -n com.wbd.stream/com.wbd.beam.BeamActivity", "HBO Max"),
        DISNEY_PLUS("am start -a android.intent.action.VIEW -n com.disney.disneyplus/com.bamtechmedia.dominguez.main.MainActivity", "Disney+"),
        PRIME_VIDEO("am start -a android.intent.action.MAIN -n com.amazon.amazonvideo.livingroom/com.amazon.ignition.IgnitionActivity", "Prime Video"),
        YOUTUBE("am start -a com.google.android.youtube.tv com.google.android.youtube.tv/com.google.android.apps.youtube.tv.activity.ShellActivity", "YouTube")
    }

    override fun execute(p: App, project: Project, device: IDevice): String {

        device.executeShellCommandWithTimeout(p.command, ShellOutputReceiver(), MIN_TIME_TO_OUTPUT_RESPONSE)

        return "Opened ${p.appName}"
    }
}
