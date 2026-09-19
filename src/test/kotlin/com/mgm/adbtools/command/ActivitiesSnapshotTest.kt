package com.mgm.adbtools.command

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Line formats below are taken verbatim from a real `adb shell dumpsys activity activities`
 * capture (Pixel 4a, Android 13) so the parser is exercised against the real dumpsys grammar,
 * not an invented one.
 */
class ActivitiesSnapshotTest {

    @Test
    fun `small input - single visible task with a live process`() {
        val dump = """
            |  * Task{1fe28da #149 type=standard A=10022:com.example.testcode U=0 visible=true visibleRequested=true mode=fullscreen translucent=false sz=1}
            |    * Hist  #0: ActivityRecord{113762949 u0 com.example.testcode/.MainActivity t149}
            |      packageName=com.example.testcode processName=com.example.testcode
            |      app=ProcessRecord{abc123 9639:com.example.testcode/u0a22}
        """.trimMargin()

        val entries = ActivitiesSnapshot.parse(dump)

        assertEquals(1, entries.size)
        val entry = entries.single()
        assertEquals("com.example.testcode", entry.appPackage)
        assertEquals("com.example.testcode.MainActivity", entry.activity)
        assertEquals(0, entry.histPosition)
        assertTrue(entry.isCurrent)
        assertTrue(entry.hasLiveProcess)
    }

    @Test
    fun `small input - background task with a live process is not current`() {
        val dump = """
            |  * Task{8e35e20 #118 type=home I=com.android.launcher3/.uioverrides.QuickstepLauncher U=0 rootTaskId=1 visible=false visibleRequested=false mode=fullscreen translucent=true sz=1}
            |    * Hist  #0: ActivityRecord{48111178 u0 com.android.launcher3/.uioverrides.QuickstepLauncher t118}
            |      app=ProcessRecord{def456 28606:com.android.launcher3/u0a187}
        """.trimMargin()

        val entry = ActivitiesSnapshot.parse(dump).single()

        assertEquals("com.android.launcher3", entry.appPackage)
        assertFalse(entry.isCurrent)
        assertTrue(entry.hasLiveProcess)
    }

    @Test
    fun `small input - activity with no ProcessRecord line is reported as having no live process`() {
        val dump = """
            |  * Task{64e19b1 #130 type=standard A=10281:com.fortinet.forticlient_vpn U=0 visible=false visibleRequested=false mode=fullscreen translucent=true sz=1}
            |    * Hist  #0: ActivityRecord{163988568 u0 com.fortinet.forticlient_vpn/forticlient.start.bringtofront.BringToFrontStartActivity t130}
            |      packageName=com.fortinet.forticlient_vpn processName=com.fortinet.forticlient_vpn
        """.trimMargin()

        val entry = ActivitiesSnapshot.parse(dump).single()

        assertFalse(entry.hasLiveProcess)
    }

    @Test
    fun `typical input - multiple Hist entries under the same task are both captured with correct positions`() {
        val dump = """
            |  * Task{3b1231e #164 type=standard A=10232:com.google.android.apps.walletnfcrel U=0 visible=false visibleRequested=false mode=fullscreen translucent=true sz=2}
            |    * Hist  #1: ActivityRecord{30095969 u0 com.google.android.apps.walletnfcrel/com.google.android.apps.wallet.infrastructure.account.selector.expresssignin.ExpressSignInActivity t164}
            |      app=ProcessRecord{ghi789 5000:com.google.android.apps.walletnfcrel/u0a232}
            |    * Hist  #0: ActivityRecord{21302105 u0 com.google.android.apps.walletnfcrel/com.google.commerce.tapandpay.android.wallet.WalletActivity t164}
            |      app=ProcessRecord{ghi789 5000:com.google.android.apps.walletnfcrel/u0a232}
        """.trimMargin()

        val entries = ActivitiesSnapshot.parse(dump)

        assertEquals(2, entries.size)
        assertEquals(setOf(0, 1), entries.map { it.histPosition }.toSet())
        assertTrue(entries.all { it.hasLiveProcess })
        assertTrue(entries.all { it.appPackage == "com.google.android.apps.walletnfcrel" })
    }

    @Test
    fun `typical input - a realistic multi-task dump resolves package, position, current and live-process independently per task`() {
        val dump = """
            |  * Task{1fe28da #149 type=standard A=10022:com.example.testcode U=0 visible=true visibleRequested=true mode=fullscreen translucent=false sz=1}
            |    * Hist  #0: ActivityRecord{113762949 u0 com.example.testcode/.MainActivity t149}
            |      app=ProcessRecord{abc123 9639:com.example.testcode/u0a22}
            |  * Task{5c63f26 #1 type=home U=0 visible=false visibleRequested=false mode=fullscreen translucent=false sz=1}
            |    * Task{8e35e20 #118 type=home I=com.android.launcher3/.uioverrides.QuickstepLauncher U=0 rootTaskId=1 visible=false visibleRequested=false mode=fullscreen translucent=true sz=1}
            |      * Hist  #0: ActivityRecord{48111178 u0 com.android.launcher3/.uioverrides.QuickstepLauncher t118}
            |        app=ProcessRecord{def456 28606:com.android.launcher3/u0a187}
            |  * Task{64e19b1 #130 type=standard A=10281:com.fortinet.forticlient_vpn U=0 visible=false visibleRequested=false mode=fullscreen translucent=true sz=1}
            |    * Hist  #0: ActivityRecord{163988568 u0 com.fortinet.forticlient_vpn/forticlient.start.bringtofront.BringToFrontStartActivity t130}
            |      packageName=com.fortinet.forticlient_vpn processName=com.fortinet.forticlient_vpn
        """.trimMargin()

        val entries = ActivitiesSnapshot.parse(dump)

        assertEquals(3, entries.size)
        val byPackage = entries.associateBy { it.appPackage }
        assertTrue(byPackage.getValue("com.example.testcode").isCurrent)
        assertTrue(byPackage.getValue("com.example.testcode").hasLiveProcess)
        assertFalse(byPackage.getValue("com.android.launcher3").isCurrent)
        assertTrue(byPackage.getValue("com.android.launcher3").hasLiveProcess)
        assertFalse(byPackage.getValue("com.fortinet.forticlient_vpn").hasLiveProcess)
    }

    @Test
    fun `large input - many tasks parse correctly and quickly`() {
        val taskCount = 500
        val dump = buildString {
            repeat(taskCount) { i ->
                appendLine("  * Task{hash$i #$i type=standard A=10000:com.example.app$i U=0 visible=false visibleRequested=false mode=fullscreen translucent=true sz=1}")
                appendLine("    * Hist  #0: ActivityRecord{recordhash$i u0 com.example.app$i/.MainActivity t$i}")
                if (i % 2 == 0) {
                    appendLine("      app=ProcessRecord{prochash$i ${10000 + i}:com.example.app$i/u0a1$i}")
                }
            }
        }

        val start = System.nanoTime()
        val entries = ActivitiesSnapshot.parse(dump)
        val elapsedMs = (System.nanoTime() - start) / 1_000_000.0

        assertEquals(taskCount, entries.size)
        assertEquals(taskCount / 2, entries.count { it.hasLiveProcess })
        assertTrue("expected parsing $taskCount synthetic tasks to take under 500ms, took ${elapsedMs}ms", elapsedMs < 500)
    }
}
