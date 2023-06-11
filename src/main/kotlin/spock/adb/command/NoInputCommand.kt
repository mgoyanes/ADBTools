package spock.adb.command

import com.android.ddmlib.IDevice

interface NoInputCommand<out R> {
    @Throws(Exception::class)
    fun execute(device: IDevice): R
}
