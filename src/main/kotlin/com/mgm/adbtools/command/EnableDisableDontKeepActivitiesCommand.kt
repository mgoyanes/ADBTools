package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.mgm.adbtools.areDontKeepActivitiesEnabled
import com.mgm.adbtools.dumpUiHierarchy
import com.mgm.adbtools.getScreenSize
import com.mgm.adbtools.pressBack
import com.mgm.adbtools.scrollToBottomBlindCommand
import com.mgm.adbtools.scrollUpCommand
import com.mgm.adbtools.tapThenPressBack

class EnableDisableDontKeepActivitiesCommand : NoInputCommand<String> {

    companion object {
        private const val SETTING_TITLE = "Don't keep activities"
        private const val MAX_SCROLL_ATTEMPTS = 15
        private const val SCREEN_LOAD_DELAY_MS = 600L

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

        device.tapThenPressBack(target.first, target.second)

        return when (device.areDontKeepActivitiesEnabled()) {
            DontKeepActivitiesState.ENABLED -> "Enabled \"$SETTING_TITLE\""
            DontKeepActivitiesState.DISABLED -> "Disabled \"$SETTING_TITLE\""
        }
    }

    // Scanning down from the top takes ~7-8 scroll-and-dump cycles to reach "Don't keep
    // activities" (Apps section); jumping to the bottom with cheap blind flings first and
    // scanning upward only takes ~2, since the trailing content past it is much shorter.
    private fun locateSetting(device: IDevice): Pair<Int, Int>? {
        val (width, height) = device.getScreenSize()
        val descendToBottom = device.scrollToBottomBlindCommand(width, height)
        val scrollUp = device.scrollUpCommand(width, height)

        repeat(MAX_SCROLL_ATTEMPTS) { attempt ->
            val precedingCommand = if (attempt == 0) descendToBottom else scrollUp
            findNodeCenter(device.dumpUiHierarchy(precedingCommand))?.let { return it }
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
