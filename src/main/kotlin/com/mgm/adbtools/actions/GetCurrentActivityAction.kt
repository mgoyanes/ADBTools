package com.mgm.adbtools.actions

import com.android.ddmlib.IDevice
import com.mgm.adbtools.AdbController

class GetCurrentActivityAction : BaseAction() {
    override fun performAction(controller: AdbController, device: IDevice) {
        controller.currentActivity(device)
    }
}
