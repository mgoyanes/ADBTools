package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout

class WindowAnimatorScaleCommand : Command<String, String> {

    companion object {
        fun getWindowAnimatorScaleIndex(scale: String?): String = scale ?: "0.0"
    }

    override fun execute(p: String, project: Project, device: IDevice): String {
        val shellOutputReceiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout("settings put global window_animation_scale $p", shellOutputReceiver)

        return "Set Window Animator Scale to $p"
    }
}
