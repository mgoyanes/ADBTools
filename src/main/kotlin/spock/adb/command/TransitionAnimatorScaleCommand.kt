package spock.adb.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import spock.adb.ShellOutputReceiver
import spock.adb.executeShellCommandWithTimeout

class TransitionAnimatorScaleCommand : Command<String, String> {

    companion object {
        fun getTransitionAnimatorScaleIndex(scale: String?): String = scale ?: "0.0"
    }

    override fun execute(p: String, project: Project, device: IDevice): String {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("settings put global transition_animation_scale $p", shellOutputReceiver)

        return "Set Transition Animator Scale to $p"
    }
}
