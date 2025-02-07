package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.executeShellCommandWithTimeout

class InputOnDeviceCommand : Command<String, String> {

    override fun execute(p: String, project: Project, device: IDevice): String {
        device.executeShellCommandWithTimeout("input text '$p'", ShellOutputReceiver())
        return "Input on device $p"
    }
}
