package wayzer.user.ext

import java.rmi.Remote
import java.rmi.RemoteException

interface AuthCache : Remote {
    @Throws(RemoteException::class)
    fun get(uid: String, usid: String, ip: String): String?
    @Throws(RemoteException::class)
    fun put(uid: String, usid: String, ip: String, gid: String)
}