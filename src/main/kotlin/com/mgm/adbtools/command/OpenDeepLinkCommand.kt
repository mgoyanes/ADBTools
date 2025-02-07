package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout

class OpenDeepLinkCommand : Command<String, String> {

    override fun execute(p: String, project: Project, device: IDevice): String {
        device.executeShellCommandWithTimeout("am start -a android.intent.action.VIEW -d \"$p\"", ShellOutputReceiver())
        return "Open DeepLink $p"
    }
}
