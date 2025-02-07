package com.mgm.adbtools.debugger

import com.android.ddmlib.Client
import com.android.ddmlib.IDevice
import com.android.tools.idea.execution.common.debug.AndroidDebugger
import com.android.tools.idea.execution.common.debug.AndroidDebuggerState
import com.android.tools.idea.execution.common.debug.DebugSessionStarter
import com.android.tools.idea.execution.common.processhandler.AndroidProcessHandler
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.AppExecutorUtil
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.joor.Reflect
import org.joor.Reflect.on


class Debugger(
    private val project: Project,
    private val device: IDevice,
    private val packageName: String,
) {
        fun attach() {
            var client: Client? = null
            waitUntil {
                client = device.getClient(packageName)
                AndroidDebugger.EP_NAME.extensions.isNotEmpty() && client != null
            }
            for (androidDebugger in AndroidDebugger.EP_NAME.extensions) {
                if (androidDebugger.supportsProject(project)) {
                    AppExecutorUtil.getAppExecutorService().execute {
                        closeOldSessionAndRun(androidDebugger, device.getClient(packageName) ?: client!!)
                    }
                    break
                }
            }
        }

        private fun closeOldSessionAndRun(androidDebugger: AndroidDebugger<AndroidDebuggerState>, client: Client) {
            terminateRunSessions(client)

            GlobalScope.launch {
                DebugSessionStarter.attachDebuggerToClientAndShowTab(
                    project,
                    client,
                    androidDebugger,
                    androidDebugger.createState()
                )
            }
        }

        // Disconnect any active run sessions to the same client
        private fun terminateRunSessions(selectedClient: Client) {
            TerminateRunSession(selectedClient, project).get()
        }
    }

class TerminateRunSession(
    private val selectedClient: Client,
    private val project: Project
) : BackwardCompatibleGetter<Unit>() {
    override fun getCurrentImplementation() {
        val pid = selectedClient.clientData.pid
        // find if there are any active run sessions to the same client, and terminate them if so
        for (handler in ExecutionManager.getInstance(project).getRunningProcesses()) {
            if (handler is AndroidProcessHandler) {
                val client = handler.getClient(selectedClient.device)
                if (client != null && client.clientData.pid == pid) {
                    handler.detachProcess()
                    handler.notifyTextAvailable(
                        "Disconnecting run session: a new debug session will be established.\n",
                        ProcessOutputTypes.STDOUT
                    )
                    break
                }
            }
        }
    }

    override fun getPreviousImplementation() {
        val pid = pidFrom(selectedClient)
        // find if there are any active run sessions to the same client, and terminate them if so
        for (handler in RunningProcessesGetter(project).get()) {
            if (handler is AndroidProcessHandler) {
                val device = on(selectedClient).call("getDevice").get<IDevice>()
                val client = handler.getClient(device)
                if (client != null && pidFrom(client) == pid) {
                    handler.detachProcess()
                    handler.notifyTextAvailable(
                        "Disconnecting run session: a new debug session will be established.\n",
                        ProcessOutputTypes.STDOUT
                    )
                    break
                }
            }
        }

    }

    private fun pidFrom(client: Client) = on(client).call("getClientData").call("getPid").get<Int>()!!
}



private class RunningProcessesGetter(
    val project: Project
) : BackwardCompatibleGetter<Array<ProcessHandler>>() {
    override fun getCurrentImplementation(): Array<ProcessHandler> {
        return ExecutionManager.getInstance(project).getRunningProcesses()
    }

    override fun getPreviousImplementation(): Array<ProcessHandler> {
        return on<ExecutionManager>().call("getInstance", project).call("getRunningProcesses").get()
    }
}

/**
 * Abstracts the logic to call the current implementation and fall back on reflection for previous versions
 */
abstract class BackwardCompatibleGetter<T> {
    fun get(): T {
        return try {
            getCurrentImplementation()
        } catch (error: LinkageError) {
            getPreviousImplementation()
        } catch (e: Throwable) {
            if (isReflectiveException(e)) {
                getPreviousImplementation()
            } else {
                throw RuntimeException(e)
            }
        }
    }

    private fun isReflectiveException(t: Throwable): Boolean {
        return t is NoSuchFieldException ||
                t is LinkageError ||
                t is NoSuchMethodException
    }

    abstract fun getCurrentImplementation(): T

    abstract fun getPreviousImplementation(): T
}

fun waitUntil(timeoutMillis: Long = 30000L, step: Long = 100L, condition: () -> Boolean) {
    val endTime = System.currentTimeMillis() + timeoutMillis
    while (System.currentTimeMillis() < endTime) {
        if (condition()) {
            return
        }
        Thread.sleep(step)
    }
}

inline fun <reified T> on(): Reflect = on(T::class.java)
