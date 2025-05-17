package coreLibrary.extApi

import java.rmi.Remote
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject


val port by config.key(10099, "RPC,监听端口")

val host: String? = System.getenv("RPC_HOST")
val isMaster = host == null

lateinit var registry: Registry
onEnable {
    if (isMaster) {
        registry = LocateRegistry.createRegistry(port)
        logger.info("RPC server started on port $port")
        onDisable {
            UnicastRemoteObject.unexportObject(registry, true)
            logger.info("RPC server stopped")
        }
    } else {
        LocateRegistry.getRegistry(host)
        logger.info("RPC started as client, host $host")
    }
}

inline fun <reified T : Remote> get(): T = get(T::class.java) as T
inline fun <reified T : Remote> register(noinline factory: () -> T) = register(T::class.java, factory)

fun <T : Remote> get(inf: Class<T>): Remote {
    return registry.lookup(inf.name)
}

fun <T : Remote> register(inf: Class<T>, factory: () -> T) {
    check(inf.isInterface && Remote::class.java.isAssignableFrom(inf)) {
        "T must be a Remote interface"
    }
    val name = inf.name
    if (!isMaster) {
        logger.info("Ignore RPC service, not master: $name")
        return
    }
    val service = factory()
    registry.bind(name, service)
    logger.info("RPC service registered: $name")
    service.thisContextScript().onDisable {
        registry.unbind(name)
    }
}