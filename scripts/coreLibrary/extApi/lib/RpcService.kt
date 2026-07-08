package coreLib.extApi

import java.rmi.Remote

//@file:Depends("coreLibrary/extApi/rpcService")
interface RpcService {
    val isMaster: Boolean
    fun <T : Remote> get(inf: Class<T>): Remote
    fun <T : Remote> register(inf: Class<T>, factory: () -> T)
}

inline fun <reified T : Remote> RpcService.get(): T = get(T::class.java) as T
inline fun <reified T : Remote> RpcService.register(noinline factory: () -> T) = register(T::class.java, factory)
