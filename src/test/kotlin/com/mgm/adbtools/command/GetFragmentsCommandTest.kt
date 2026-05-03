package com.mgm.adbtools.command

import com.android.ddmlib.IDevice
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.models.BackStackData
import com.mgm.adbtools.models.FragmentData
import com.mgm.adbtools.parser.DumpsysParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class GetFragmentsCommandTest {

    private lateinit var command: GetApplicationBackStackCommand
    private lateinit var mockDevice: IDevice

    @Before
    fun setUp() {
        command = GetApplicationBackStackCommand()
        mockDevice = mock()
    }

    @Test
    fun `execute returns empty list when output starts with Unknown command`() {
        doAnswer { invocation ->
            val receiver = invocation.getArgument<ShellOutputReceiver>(1)
            receiver.addOutput("Unknown command".toByteArray(), 0, "Unknown command".length)
            null
        }.whenever(mockDevice).executeShellCommand(any(), any(), eq(5L), eq(java.util.concurrent.TimeUnit.SECONDS))

        val result = command.execute(listOf("com.test.app"), mockDevice)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse dumpsys output and verify pretty print format`() {
        val dumpsysOutput = File("src/test/kotlin/com/mgm/adbtools/command/dumpsys.txt").readText().trimIndent()
        val parser = DumpsysParser()

        val result = parser.parse(dumpsysOutput)
        val prettyPrint = with(parser) { result.prettyPrint() }

        val expectedOutput = """
        [0]-MainActivity [Activity]
        	[0]-HomeFragment [Fragment]
        		[0]-HomeStartFragment [Fragment]
        			[0]-RechargeCategoryFragment [Fragment]
        		[1]-HomeManagementFragment [Fragment]
        			[0]-ReportsFragment [Fragment]
        			[1]-SubAgentsFragment [Fragment]
        			[2]-SellersFragment [Fragment]
        		[2]-HomeReportsFragment [Fragment]
        		[3]-HomeProfileFragment [Fragment]
        [1]-MainActivity [Activity]
        	[0]-HomeFragment [Fragment]
        	[1]-MapsFragment [Fragment]
        	[2]-ManualSearchFragment [Fragment]
    """.trimIndent()

        println(prettyPrint)

        assertEquals(expectedOutput, prettyPrint)
    }

    @Test
    fun `parse adb output and verify pretty print format`() {
        val adbOutput = File("src/test/kotlin/com/mgm/adbtools/command/adb.txt").readText().trimIndent()
        val parser = DumpsysParser()

        val result = parser.parse(adbOutput)
        val prettyPrint = with(parser) { result.prettyPrint() }

        val expectedOutput = """
        [0]-MainActivity [Activity]
        	[0]-FirstFragment [Fragment]
        	[1]-SecondFragment [Fragment]
        		[0]-TestInnerFragment [Fragment]
    """.trimIndent()

        println(prettyPrint)

        assertEquals(expectedOutput, prettyPrint)
    }

    @Test
    fun `parse adb2 output and verify pretty print format`() {
        val adbOutput = File("src/test/kotlin/com/mgm/adbtools/command/adb2.txt").readText().trimIndent()
        val parser = DumpsysParser()

        val result = parser.parse(adbOutput)
        val prettyPrint = with(parser) { result.prettyPrint() }

        val expectedOutput = """
        [0]-MainActivity [Activity]
        	[0]-HomeFragment [Fragment]
        	[1]-MapsFragment [Fragment]
        	[2]-AddressConfirmationFragment [Fragment]
        	[3]-PlansListFragment [Fragment]
        	[4]-PackagesListFragment [Fragment]
        	[5]-PackageDetailFragment [Fragment]
        	[6]-ExtraServicesFragment [Fragment]
        		[0]-LoadingFullDialogFragment [Fragment]
        	[7]-DevicesListFragment [Fragment]
        	[8]-ClientSearchFragment [Fragment]
        		[0]-LoadingFullDialogFragment [Fragment]
        	[9]-ClientResultsFragment [Fragment]
        	[10]-DigitalContractFragment [Fragment]
        """.trimIndent()

        println(prettyPrint)

        assertEquals(expectedOutput, prettyPrint)
    }

    @Test
    fun `parse adb3 output and verify pretty print format`() {
        val adbOutput = File("src/test/kotlin/com/mgm/adbtools/command/adb3.txt").readText().trimIndent()
        val parser = DumpsysParser()

        val result = parser.parse(adbOutput)
        val prettyPrint = with(parser) { result.prettyPrint() }

        val expectedOutput = """
        [0]-MainActivity [Activity]
        	[0]-HomeFragment [Fragment]
        		[0]-HomeStartFragment [Fragment]
        			[0]-RechargeCategoryFragment [Fragment]
        		[1]-HomeManagementFragment [Fragment]
        			[0]-ReportsFragment [Fragment]
        			[1]-SellersFragment [Fragment]
        			[2]-SubAgentsFragment [Fragment]
        		[2]-HomeReportsFragment [Fragment]
        		[3]-HomeProfileFragment [Fragment]
        [1]-MainActivity [Activity]
        	[0]-HomeFragment [Fragment]
        	[1]-MapsFragment [Fragment]
        	[2]-AddressConfirmationFragment [Fragment]
        	[3]-PlansListFragment [Fragment]
        	[4]-PackagesListFragment [Fragment]
        """.trimIndent()

        println(prettyPrint)

        assertEquals(expectedOutput, prettyPrint)
    }

    @Test
    fun `parse adb4 output and verify pretty print format`() {
        val adbOutput = File("src/test/kotlin/com/mgm/adbtools/command/adb4.txt").readText().trimIndent()
        val parser = DumpsysParser()

        val result = parser.parse(adbOutput)
        val prettyPrint = with(parser) { result.prettyPrint() }

        val expectedOutput = """
        [0]-MainActivity [Activity]
        	[0]-HomeFragment [Fragment]
        		[0]-HomeStartFragment [Fragment]
        			[0]-RechargeCategoryFragment [Fragment]
        		[1]-NavHostFragment [Fragment]
        			[0]-HomeManagementFragment [Fragment]
        				[0]-ReportsFragment [Fragment]
        				[1]-SellersFragment [Fragment]
        				[2]-SubAgentsFragment [Fragment]
        			[1]-SubAgentDetailsFragment [Fragment]
        		[2]-HomeReportsFragment [Fragment]
        		[3]-HomeProfileFragment [Fragment]
        """.trimIndent()

        println(prettyPrint)

        assertEquals(expectedOutput, prettyPrint)
    }
}
