package com.mgm.adbtools.perf

import com.intellij.openapi.diagnostic.Logger
import java.util.Locale

/**
 * Timing instrumentation for the "current backstack activities" and "Open current app backstack"
 * actions only (see AdbToolsViewer's activitiesBackStackButton / currentAppBackStackButton
 * listeners, and AdbControllerImp.currentBackStack / currentApplicationBackStack).
 *
 * Disabled by default. Enable by launching the sandbox IDE with
 * -Dadbtools.perf.trace=true (a JVM system property, not a Registry key or plugin.xml change).
 *
 * When disabled - or when no run has been started via [startRun] - [time] and [timeRepeated]
 * are pure passthroughs (one boolean check, no timing, no allocation), so behaviour and timing
 * of the instrumented code are unchanged.
 */
object PerfTrace {

    private const val TAG = "ADBTOOLS_PERF"
    private val logger = Logger.getInstance(PerfTrace::class.java)

    val enabled: Boolean
        get() = System.getProperty("adbtools.perf.trace")?.equals("true", ignoreCase = true) == true ||
            System.getenv("ADBTOOLS_PERF_TRACE")?.equals("true", ignoreCase = true) == true

    private class Run(val action: String, val clickAtMillis: Long) {
        val startNanos: Long = System.nanoTime()
        val startMillis: Long = System.currentTimeMillis()
        val spans = LinkedHashMap<String, Long>()
        val repeated = LinkedHashMap<String, MutableList<Long>>()
    }

    private var active: Run? = null

    /**
     * Call at the top of the button's ActionListener, before any work happens.
     * [clickAtMillis] should be the originating ActionEvent.getWhen() so the gap between the
     * physical click and this call (EDT queue wait) can be measured separately from work time.
     */
    fun startRun(action: String, clickAtMillis: Long) {
        if (!enabled) return
        active = Run(action, clickAtMillis)
    }

    /** Times a single-shot named span. No-op passthrough when disabled or no run is active. */
    fun <T> time(name: String, block: () -> T): T {
        val run = active
        if (!enabled || run == null) return block()
        val start = System.nanoTime()
        try {
            return block()
        } finally {
            run.spans[name] = System.nanoTime() - start
        }
    }

    /** Times one occurrence of a span that repeats in a loop (e.g. one adb round trip per activity). */
    fun <T> timeRepeated(name: String, block: () -> T): T {
        val run = active
        if (!enabled || run == null) return block()
        val start = System.nanoTime()
        try {
            return block()
        } finally {
            run.repeated.getOrPut(name) { mutableListOf() }.add(System.nanoTime() - start)
        }
    }

    /** Call once the popup has been shown. Emits exactly one log line for the whole run. */
    fun finishRun() {
        val run = active ?: return
        active = null
        if (!enabled) return

        val totalNanos = System.nanoTime() - run.startNanos
        val edtQueueWaitMs = (run.startMillis - run.clickAtMillis).coerceAtLeast(0)

        val line = buildString {
            append(TAG)
            append(" action=").append(run.action)
            append(" edt_queue_wait_ms=").append(edtQueueWaitMs)
            append(" total_ms=").append(totalNanos.ms())

            run.spans.forEach { (name, nanos) ->
                append(' ').append(name).append("_ms=").append(nanos.ms())
            }

            run.repeated.forEach { (name, samplesNanos) ->
                val samplesMs = samplesNanos.map { it / 1_000_000.0 }
                append(' ').append(name).append("_count=").append(samplesMs.size)
                append(' ').append(name).append("_total_ms=").append(samplesMs.sum().fmt())
                append(' ').append(name).append("_min_ms=").append((samplesMs.minOrNull() ?: 0.0).fmt())
                append(' ').append(name).append("_max_ms=").append((samplesMs.maxOrNull() ?: 0.0).fmt())
                append(' ').append(name).append("_avg_ms=").append((if (samplesMs.isNotEmpty()) samplesMs.average() else 0.0).fmt())
                append(' ').append(name).append("_each_ms=").append(samplesMs.joinToString(",") { it.fmt() })
            }
        }

        logger.info(line)
    }

    private fun Long.ms(): String = (this / 1_000_000.0).fmt()

    private fun Double.fmt(): String = String.format(Locale.ROOT, "%.2f", this)
}
