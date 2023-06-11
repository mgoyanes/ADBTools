package spock.adb.command

import com.android.ddmlib.IDevice

interface ListCommand<in P, out R> {
    @Throws(Exception::class)
    fun execute(list: List<P>, device: IDevice): R
}
