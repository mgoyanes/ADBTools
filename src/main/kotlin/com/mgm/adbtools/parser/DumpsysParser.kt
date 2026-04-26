package com.mgm.adbtools.parser

import com.mgm.adbtools.models.ActivityData
import com.mgm.adbtools.models.BackStackData
import com.mgm.adbtools.models.FragmentData

class DumpsysParser {

    private enum class Section { NONE, ACTIVE, ADDED, BACKSTACK, OPS }

    private enum class OpType { ADD, REMOVE, REPLACE, SET_PRIMARY_NAV, UNSET_PRIMARY_NAV, OTHER }

    private data class ActivityBlock(
        val component: String,
        val startIndex: Int,
        val lines: List<String> = emptyList()
    )

    private data class FragmentInfo(
        val name: String,
        val key: String,
        val who: String,
        val tag: String
    )

    private data class Fragment(
        val name: String,
        val key: String,
        val who: String = "",
        val tag: String = "",
        val bottomNavIndex: Int? = null,
        var childManager: FragmentManager? = null
    ) {
        val isNavHost: Boolean get() = name == "NavHostFragment"
        val isReportFragment: Boolean get() = name == "ReportFragment"
    }

    private data class FragmentManager(
        val fragments: MutableMap<String, Fragment> = mutableMapOf(),
        val activeKeys: MutableList<String> = mutableListOf(),
        val addedKeys: MutableList<String> = mutableListOf(),
        val backStack: MutableList<BackStackEntry> = mutableListOf()
    )

    private data class BackStackEntry(
        val index: Int,
        val operations: MutableList<Operation> = mutableListOf()
    )

    private data class Operation(
        val type: OpType,
        val fragmentKey: String?
    )

    fun parse(dumpsysOutput: String, vararg moreOutputs: String): BackStackData {
        val allOutputs = listOf(dumpsysOutput) + moreOutputs.toList()
        val results = allOutputs.map { parseSingle(it) }

        val appPkg = results.firstNotNullOfOrNull { it.appPackage.takeIf(String::isNotBlank) } ?: ""

        val merged = results
            .flatMap { it.activitiesList }
            .mapIndexed { idx, act -> act.copy(activityStackPosition = idx) }

        return BackStackData(appPkg, merged)
    }

    private fun parseSingle(dumpsysOutput: String): BackStackData =
        parseSingle(dumpsysOutput.lines())

    private fun parseSingle(lines: List<String>): BackStackData {
        val appPkg = extractAppPackage(lines)
        val activities = extractActivityBlocks(lines).mapIndexed { idx, block ->
            parseActivityBlock(block, idx)
        }
        return BackStackData(appPkg, activities)
    }

    private fun extractAppPackage(lines: List<String>): String =
        lines.firstOrNull { it.trimStart().startsWith("TASK ") }
            ?.let { TASK_REGEX.find(it)?.groupValues?.get(1) }
            ?: ""

    private fun extractActivityBlocks(lines: List<String>): List<ActivityBlock> {
        val blockStarts = mutableListOf<ActivityBlock>()
        lines.forEachIndexed { index, line ->
            ACTIVITY_REGEX.find(line)?.let { match ->
                blockStarts.add(ActivityBlock(match.groupValues[1], index))
            }
        }
        return blockStarts.mapIndexed { idx, block ->
            val endIndex = blockStarts.getOrNull(idx + 1)?.startIndex ?: lines.size
            block.copy(lines = lines.subList(block.startIndex, endIndex))
        }
    }

    private fun parseActivityBlock(block: ActivityBlock, index: Int): ActivityData {
        val activityName = block.component
            .substringAfterLast('/')
            .removePrefix(".")
            .substringAfterLast('.')

        val isKilled = block.lines.any { "mFinished=true" in it || "mDestroyed=true" in it }

        val fragmentActivityStart = block.lines.indexOfFirst { "Local FragmentActivity" in it }
        val fragments = if (fragmentActivityStart >= 0) {
            val fragmentLines = block.lines.subList(fragmentActivityStart, block.lines.size)
            val parseState = ParseState(fragmentLines)
            val manager = parseManager(parseState, stopIndent = null)
            buildFragmentList(manager)
        } else {
            emptyList()
        }

        return ActivityData(
            activity = activityName,
            activityStackPosition = index,
            isKilled = isKilled,
            fragment = fragments
        )
    }

    private class ParseState(val lines: List<String>, var position: Int = 0)

    private fun parseManager(state: ParseState, stopIndent: Int?): FragmentManager {
        val manager = FragmentManager()
        var currentSection = Section.NONE
        var currentBackStackEntry: BackStackEntry? = null

        while (state.position < state.lines.size) {
            val line = state.lines[state.position]
            val indent = line.indentLevel()

            if (stopIndent != null && indent <= stopIndent) break

            val trimmed = line.trim()

            currentSection = detectSectionChange(trimmed, currentSection)

            when (currentSection) {
                Section.ACTIVE -> {
                    if (FRAGMENT_HEADER_REGEX.containsMatchIn(line)) {
                        val fragment = parseFragment(state, stopIndent)
                        manager.fragments[fragment.key] = fragment
                        manager.activeKeys += fragment.key
                        continue
                    }
                }
                Section.ADDED -> {
                    ADDED_FRAGMENT_REGEX.find(line)?.let { match ->
                        val info = extractFragmentInfo(match)
                        manager.addedKeys += info.key
                        manager.fragments.getOrPut(info.key) { Fragment(info.name, info.key, info.who, info.tag) }
                    }
                }
                Section.BACKSTACK -> {
                    BACKSTACK_ENTRY_REGEX.find(line)?.let { match ->
                        currentBackStackEntry = BackStackEntry(match.groupValues[1].toInt())
                        manager.backStack += currentBackStackEntry
                    }
                }
                Section.OPS -> {
                    BACKSTACK_ENTRY_REGEX.find(line)?.let { match ->
                        currentBackStackEntry = BackStackEntry(match.groupValues[1].toInt())
                        manager.backStack += currentBackStackEntry
                        currentSection = Section.BACKSTACK
                    } ?: OP_REGEX.find(line)?.let { match ->
                        val op = parseOperation(match)
                        currentBackStackEntry?.operations?.add(op)

                        op.fragmentKey?.let { key ->
                            FRAGMENT_HEADER_REGEX.find(match.groupValues[2])?.let { fragMatch ->
                                val info = extractFragmentInfo(fragMatch)
                                manager.fragments.getOrPut(key) { Fragment(info.name, key, info.who, info.tag) }
                            }
                        }
                    }
                }
                Section.NONE -> {}
            }

            if (trimmed.startsWith("Operations:")) {
                currentSection = Section.OPS
            }

            state.position++
        }

        return manager
    }

    private fun parseFragment(state: ParseState, managerStopIndent: Int?): Fragment {
        val headerLine = state.lines[state.position]
        val headerIndent = headerLine.indentLevel()

        val match = FRAGMENT_HEADER_REGEX.find(headerLine)
            ?: return Fragment("Unknown", "unknown").also { state.position++ }

        val info = extractFragmentInfo(match)
        val bottomNavIdx = BOTTOM_NAV_REGEX.find(info.tag)?.groupValues?.get(1)?.toIntOrNull()

        val fragment = Fragment(info.name, info.key, info.who, info.tag, bottomNavIdx)
        state.position++

        while (state.position < state.lines.size) {
            val line = state.lines[state.position]
            val indent = line.indentLevel()

            if (managerStopIndent != null && indent <= managerStopIndent) break
            if (indent <= headerIndent) break

            if (line.trim().startsWith("Child FragmentManager{")) {
                val childIndent = indent
                state.position++
                fragment.childManager = parseManager(state, stopIndent = childIndent)
                continue
            }

            state.position++
        }

        return fragment
    }

    private fun detectSectionChange(trimmed: String, current: Section): Section = when {
        "Active Fragments:" in trimmed -> Section.ACTIVE
        trimmed.startsWith("Added Fragments:") -> Section.ADDED
        trimmed.startsWith("Back Stack:") -> Section.BACKSTACK
        isSectionEnd(trimmed) -> Section.NONE
        else -> current
    }

    private fun isSectionEnd(trimmed: String): Boolean =
        trimmed.startsWith("Back Stack Index:") ||
                trimmed.startsWith("FragmentManager misc state:")

    private fun extractFragmentInfo(match: MatchResult): FragmentInfo {
        val nameRaw = match.groupValues[1]
        val key = match.groupValues[2]
        val paren = match.groupValues.getOrNull(3)?.trim() ?: ""

        val who = paren.takeWhile { !it.isWhitespace() }
        val tag = TAG_REGEX.find(paren)?.groupValues?.get(1) ?: ""

        return FragmentInfo(nameRaw.substringAfterLast('.'), key, who, tag)
    }

    private fun parseOperation(match: MatchResult): Operation {
        val opType = when (match.groupValues[1]) {
            "ADD" -> OpType.ADD
            "REMOVE" -> OpType.REMOVE
            "REPLACE" -> OpType.REPLACE
            "SET_PRIMARY_NAV" -> OpType.SET_PRIMARY_NAV
            "UNSET_PRIMARY_NAV" -> OpType.UNSET_PRIMARY_NAV
            else -> OpType.OTHER
        }

        val rest = match.groupValues[2].trim()
        val fragmentKey = if (rest.startsWith("null")) {
            null
        } else {
            FRAGMENT_HEADER_REGEX.find(rest)?.groupValues?.get(2)
        }

        return Operation(opType, fragmentKey)
    }

    private fun String.indentLevel(): Int =
        indexOfFirst { it != ' ' }.let { if (it < 0) length else it }

    private fun buildFragmentList(manager: FragmentManager): List<FragmentData> {
        val navHostKeys = manager.activeKeys.filter { manager.fragments[it]?.isNavHost == true }

        if (navHostKeys.isNotEmpty()) {
            return navHostKeys
                .sortedWith(compareBy(
                    { manager.fragments[it]?.bottomNavIndex ?: Int.MAX_VALUE },
                    { manager.activeKeys.indexOf(it) }
                ))
                .flatMap { key ->
                    manager.fragments[key]?.childManager?.let { buildFragmentList(it) } ?: emptyList()
                }
        }

        return resolveStackOrder(manager).mapNotNull { key ->
            val fragment = manager.fragments[key] ?: return@mapNotNull null
            when {
                fragment.isReportFragment -> null
                fragment.isNavHost -> null
                else -> FragmentData(
                    fragment = fragment.name,
                    fragmentIdentifier = fragment.who,
                    innerFragments = fragment.childManager?.let { buildFragmentList(it) }?.toMutableList() ?: mutableListOf()
                )
            }
        }
    }

    private fun resolveStackOrder(manager: FragmentManager): List<String> = buildList {
        when {
            manager.backStack.isNotEmpty() -> {
                val sortedEntries = manager.backStack.sortedBy { it.index }

                sortedEntries.firstOrNull()?.let { first ->
                    first.findBaseFragment()?.let { add(it) }
                }

                sortedEntries.forEach { entry ->
                    entry.findTopFragment()?.let { key ->
                        if (isEmpty() || last() != key) add(key)
                    }
                }
            }
            manager.addedKeys.isNotEmpty() -> addAll(manager.addedKeys)
            else -> addAll(manager.activeKeys)
        }

        manager.addedKeys.lastOrNull()?.let { top ->
            if (isEmpty() || last() != top) add(top)
        }
    }

    private fun BackStackEntry.findBaseFragment(): String? =
        operations.firstOrNull { it.type == OpType.UNSET_PRIMARY_NAV && it.fragmentKey != null }?.fragmentKey
            ?: operations.firstOrNull { it.type in listOf(OpType.REMOVE, OpType.REPLACE) && it.fragmentKey != null }?.fragmentKey

    private fun BackStackEntry.findTopFragment(): String? =
        operations.firstOrNull { it.type == OpType.SET_PRIMARY_NAV && it.fragmentKey != null }?.fragmentKey
            ?: operations.lastOrNull { it.type == OpType.ADD && it.fragmentKey != null }?.fragmentKey

    fun BackStackData.prettyPrint(): String = buildString {
        fun appendFragment(f: FragmentData, index: Int, depth: Int) {
            repeat(depth) { append("\t") }
            append("[$index]-${f.fragment} [Fragment]\n")
            f.innerFragments.forEachIndexed { i, child -> appendFragment(child, i, depth + 1) }
        }

        activitiesList.forEach { act ->
            append("[${act.activityStackPosition}]-${act.activity} [Activity]\n")
            act.fragment.forEachIndexed { idx, frag -> appendFragment(frag, idx, 1) }
        }
    }.trimEnd()

    companion object {
        private val TASK_REGEX = Regex("""TASK\s+\d+:(\S+)""")
        private val ACTIVITY_REGEX = Regex("""^\s*ACTIVITY\s+(\S+)""")
        private val FRAGMENT_HEADER_REGEX = Regex("""^\s*([A-Za-z0-9_.$]+)\{([0-9a-f]+)\}(?:\s*\(([^)]+)\))?""")
        private val ADDED_FRAGMENT_REGEX = Regex("""^\s*#\d+:\s*([A-Za-z0-9_.\$]+)\{([0-9a-f]+)\}(?:\s*\(([^)]+)\))?""")
        private val BACKSTACK_ENTRY_REGEX = Regex("""^\s*#(\d+):\s*BackStackEntry\{""")
        private val OP_REGEX = Regex("""^\s*Op #\d+:\s*([A-Z_]+)\s+(.*)$""")
        private val TAG_REGEX = Regex("""\btag=([^\s)]+)""")
        private val BOTTOM_NAV_REGEX = Regex("""bottomNavigation#(\d+)""")
    }
}
