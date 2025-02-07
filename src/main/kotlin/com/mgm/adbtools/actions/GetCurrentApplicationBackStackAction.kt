package com.mgm.adbtools.actions

import com.android.ddmlib.IDevice
import com.mgm.adbtools.AdbController

class GetCurrentApplicationBackStackAction : BaseAction() {
    override fun performAction(controller: AdbController, device: IDevice) {
        controller.currentBackStack(device)
    }
}
