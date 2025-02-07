package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout

class AnimatorDurationScaleCommand : Command<String, String> {

    companion object {
        fun getAnimatorDurationScaleIndex(scale: String?): String = scale ?: "0.0"
    }

    override fun execute(p: String, project: Project, device: IDevice): String {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("settings put global animator_duration_scale $p", shellOutputReceiver)

        return "Set Animator Duration Scale to $p"
    }
}
