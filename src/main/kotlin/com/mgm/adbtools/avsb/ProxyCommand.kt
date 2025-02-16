package com.mgm.adbtools.avsb

import ProcessCommand
import com.android.ddmlib.IDevice
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.mgm.adbtools.ONE
import com.mgm.adbtools.ShellOutputReceiver
import com.mgm.adbtools.ZERO
import com.mgm.adbtools.command.InputOnDeviceCommand
import com.mgm.adbtools.executeShellCommandWithTimeout
import java.util.concurrent.TimeUnit

class ProxyCommand {

    companion object {
        const val OPEN_PROXY_SETTINGS_COMMAND = "am start -a android.settings.SETTINGS -n com.android.tv.settings/.connectivity.EditProxySettingsActivity"
        const val OPEN_NETWORK_SETTINGS_COMMAND = "am start -n com.android.tv.settings/.connectivity.NetworkActivity"
        const val FORCE_KILL_SETTINGS = "am force-stop com.android.tv.settings"
        const val DELETE = "adb shell input keyevent ${KeyEventCommand.DELETE}"
        const val CONNECTION_INFO_COMMAND = "dumpsys connectivity | grep \"type:\""
        private val CONNECTION_REGEX = Regex("""type:\s*(\w+)\[\],\s*state:\s*([\w/]+)""")
    }

    private val unknownConnection = Connection(ConnectionType.UNKNOWN, ConnectionState.UNKNOWN)


    //region Clear Proxy
    fun clearProxy(project: Project, device: IDevice): String {

        val connectionInfo = getConnectionInfo(device)

        when (connectionInfo.type) {
            ConnectionType.WIFI -> if (ConnectionState.CONNECTED == connectionInfo.state) removeWifiProxy(device, project) else return "Unknown connection state"
            ConnectionType.ETHERNET -> removeEthernetProxy(device, project)
            ConnectionType.UNKNOWN -> return "Unable to obtain connection type"
        }

        return "Proxy removed"
    }

    private fun removeEthernetProxy(device: IDevice, project: Project) {
        val receiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout(
            FORCE_KILL_SETTINGS,
            receiver,
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )

        device.executeShellCommandWithTimeout(
            OPEN_PROXY_SETTINGS_COMMAND,
            receiver,
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )

        val keyEventCommand = KeyEventCommand()
        keyEventCommand.execute(KeyEventCommand.DPAD_UP, project, device)

        keyEventCommand.execute(KeyEventCommand.OK, project, device)

        device.executeShellCommandWithTimeout(
            FORCE_KILL_SETTINGS,
            receiver,
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )
    }

    private fun removeWifiProxy(device: IDevice, project: Project) {
        openWIFIProxyScreen(device, project)

        val keyEventCommand = KeyEventCommand()

        keyEventCommand.execute(KeyEventCommand.DPAD_UP, project, device)

        keyEventCommand.execute(KeyEventCommand.OK, project, device)

        device.executeShellCommandWithTimeout(
            FORCE_KILL_SETTINGS,
            ShellOutputReceiver(),
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )
    }
    //endregion

    //region Set Proxy
    fun setProxy(hostname: String?, port: String?, project: Project, device: IDevice): String {

        requireNotNull(hostname) {
            "Invalid details"
        }

        requireNotNull(port) {
            "Invalid details"
        }

        val connectionInfo = getConnectionInfo(device)

        when (connectionInfo.type) {
            ConnectionType.WIFI ->
                if (ConnectionState.CONNECTED == connectionInfo.state) setWifiProxy(device, project, hostname, port) else return "Unknown connection state. Proxy not set"

            ConnectionType.ETHERNET ->
                if (ConnectionState.CONNECTED == connectionInfo.state) setEthernetProxy(device, project, hostname, port) else return "Unknown connection state. Proxy not set"

            ConnectionType.UNKNOWN -> return "Unable to obtain connection type. Proxy not set"
        }

        return "Proxy set"
    }

    private fun setEthernetProxy(device: IDevice, project: Project, hostname: String, port: String) {
        val receiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout(
            FORCE_KILL_SETTINGS,
            receiver,
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )

        device.executeShellCommandWithTimeout(
            OPEN_PROXY_SETTINGS_COMMAND,
            receiver,
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )

        setProxy(project, device, hostname, port)
    }

    private fun setWifiProxy(device: IDevice, project: Project, hostname: String, port: String) {

        openWIFIProxyScreen(device, project)

        val keyEventCommand = KeyEventCommand()

        keyEventCommand.execute(KeyEventCommand.DPAD_DOWN, project, device)

        keyEventCommand.execute(KeyEventCommand.OK, project, device)

        setProxy(project, device, hostname, port)
    }

    private fun setProxy(project: Project, device: IDevice, hostname: String, port: String) {
        val keyEventCommand = KeyEventCommand()

        repeat(20) {
            ProcessCommand().execute(DELETE, "Clear Contents", project, 200.toLong())
        }

        InputOnDeviceCommand().execute(hostname, project, device)

        keyEventCommand.execute(KeyEventCommand.DONE, project, device)

        repeat(10) {
            ProcessCommand().execute(DELETE, "Clear Contents", project, 200.toLong())
        }

        InputOnDeviceCommand().execute(port, project, device)

        keyEventCommand.execute(KeyEventCommand.DONE, project, device)

        CoroutineScope(Dispatchers.Unconfined).launch {
            delay(TimeUnit.SECONDS.toMillis(ONE.toLong()))
            keyEventCommand.execute(KeyEventCommand.DONE, project, device)

            device.executeShellCommandWithTimeout(
                FORCE_KILL_SETTINGS,
                ShellOutputReceiver(),
                ZERO.toLong(),
                TimeUnit.SECONDS,
            )
        }
    }
    //endregion

    private fun openWIFIProxyScreen(device: IDevice, project: Project) {
        val receiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout(
            FORCE_KILL_SETTINGS,
            receiver,
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )

        device.executeShellCommandWithTimeout(
            OPEN_NETWORK_SETTINGS_COMMAND,
            receiver,
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )

        val keyEventCommand = KeyEventCommand()

        keyEventCommand.execute(KeyEventCommand.DPAD_DOWN, project, device)

        keyEventCommand.execute(KeyEventCommand.OK, project, device)

        keyEventCommand.execute(KeyEventCommand.DPAD_DOWN, project, device)

        keyEventCommand.execute(KeyEventCommand.DPAD_DOWN, project, device)

        keyEventCommand.execute(KeyEventCommand.DPAD_DOWN, project, device)

        keyEventCommand.execute(KeyEventCommand.DPAD_DOWN, project, device)

        keyEventCommand.execute(KeyEventCommand.DPAD_DOWN, project, device)

        keyEventCommand.execute(KeyEventCommand.OK, project, device)
    }

    private fun getConnectionInfo(device: IDevice): Connection {

        val receiver = ShellOutputReceiver()
        device.executeShellCommandWithTimeout(
            CONNECTION_INFO_COMMAND,
            receiver,
            ZERO.toLong(),
            TimeUnit.SECONDS,
        )

        // Find the match in the text
        val matchResult = CONNECTION_REGEX.find(receiver.toString()) ?: return unknownConnection

        // If a match is found, extract the connection type and state
        val (connectionType, connectionState) = matchResult.destructured

        val type = ConnectionType.entries.firstOrNull { connectionType.equals(it.name, true) } ?: return unknownConnection
        val state = ConnectionState.entries.firstOrNull { connectionState.equals(it.state, true) } ?: return unknownConnection


        return Connection(type, state)
    }

    internal class Connection(val type: ConnectionType, val state: ConnectionState)

    internal enum class ConnectionType {
        WIFI,
        ETHERNET,
        UNKNOWN
    }

    internal enum class ConnectionState(val state: String?) {
        CONNECTED("CONNECTED/CONNECTED"),
        UNKNOWN(null)
    }
}
