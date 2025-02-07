package com.mgm.adbtools.actions

import com.android.ddmlib.IDevice
import com.mgm.adbtools.AdbController

class RestartAppAction : BaseAction() {
    override fun performAction(controller: AdbController, device: IDevice) {
        controller.restartApp(device)
    }
}
