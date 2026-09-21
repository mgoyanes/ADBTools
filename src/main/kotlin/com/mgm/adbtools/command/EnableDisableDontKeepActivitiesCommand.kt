package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.mgm.adbtools.areDontKeepActivitiesEnabled
import com.mgm.adbtools.dumpUiHierarchy
import com.mgm.adbtools.pressBack
import com.mgm.adbtools.scrollDown
import com.mgm.adbtools.tap

class EnableDisableDontKeepActivitiesCommand : NoInputCommand<String> {

    companion object {
        private const val SETTING_TITLE = "Don't keep activities"
        private const val MAX_SCROLL_ATTEMPTS = 15
        private const val SCREEN_LOAD_DELAY_MS = 600L
        private const val TAP_SETTLE_DELAY_MS = 500L

        private val NODE_REGEX = Regex("<node[^>]*/>")
        private val TEXT_ATTR_REGEX = Regex("text=\"([^\"]*)\"")
        private val BOUNDS_ATTR_REGEX = Regex("bounds=\"\\[(\\d+),(\\d+)]\\[(\\d+),(\\d+)]\"")
    }

    override fun execute(device: IDevice): String {
        OpenDeveloperOptionsCommand().execute(device)
        Thread.sleep(SCREEN_LOAD_DELAY_MS)

        val target = locateSetting(device) ?: run {
            device.pressBack()
            throw Exception("Could not find \"$SETTING_TITLE\" in Developer options")
        }

        device.tap(target.first, target.second)
        Thread.sleep(TAP_SETTLE_DELAY_MS)
        device.pressBack()

        return when (device.areDontKeepActivitiesEnabled()) {
            DontKeepActivitiesState.ENABLED -> "Enabled \"$SETTING_TITLE\""
            DontKeepActivitiesState.DISABLED -> "Disabled \"$SETTING_TITLE\""
        }
    }

    private fun locateSetting(device: IDevice): Pair<Int, Int>? {
        repeat(MAX_SCROLL_ATTEMPTS) { attempt ->
            findNodeCenter(device.dumpUiHierarchy())?.let { return it }
            if (attempt < MAX_SCROLL_ATTEMPTS - 1) device.scrollDown()
        }
        return null
    }

    private fun findNodeCenter(xml: String): Pair<Int, Int>? {
        val node = NODE_REGEX.findAll(xml)
            .map { it.value }
            .firstOrNull { node ->
                val text = TEXT_ATTR_REGEX.find(node)?.groupValues?.get(1)?.replace('’', '\'')
                text.equals(SETTING_TITLE, ignoreCase = true)
            } ?: return null

        val (left, top, right, bottom) = BOUNDS_ATTR_REGEX.find(node)?.destructured ?: return null
        return (left.toInt() + right.toInt()) / 2 to (top.toInt() + bottom.toInt()) / 2
    }
}
