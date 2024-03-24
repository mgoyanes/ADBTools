package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout

class FirebaseCommand : Command2<String, String, Unit> {

    companion object {
        const val NO_DEBUG_APP = ".none."
    }

    override fun execute(p: String, p2: String, project: Project, device: IDevice) {
        val shellOutputReceiver = ShellOutputReceiver()
        when (p2) {
            NO_DEBUG_APP -> {
                device.executeShellCommandWithTimeout("setprop debug.firebase.analytics.app $p", shellOutputReceiver)
                device.executeShellCommandWithTimeout("setprop log.tag.FA VERBOSE", shellOutputReceiver)
                device.executeShellCommandWithTimeout("setprop log.tag.FA-SVC VERBOSE", shellOutputReceiver)
            }

            else -> {
                device.executeShellCommandWithTimeout("setprop debug.firebase.analytics.app $NO_DEBUG_APP", shellOutputReceiver)
            }
        }
    }
}
