@file:Depends("coreLibrary/extApi/redisApi", "基于redis")
@file:Import("./lib/RemoteEvent.kt", sourceFile = true)
@file:Implement(RemoteEvent.Impl::class)

import coreLib.extApi.RedisApi
import coreLib.extApi.RemoteEvent
import redis.clients.jedis.BinaryJedisPubSub
import java.io.*
import java.lang.ref.WeakReference
import java.util.logging.Level

val group by config.key("_SA_RemoteEvent")
val classMap = mutableMapOf<String, WeakReference<Class<*>>>()

fun remoteEmit(event: RemoteEvent) = launch(Dispatchers.IO) {
    RedisApi.use {
        publish(group.toByteArray(), ByteArrayOutputStream().use {
            ObjectOutputStream(it).writeObject(event)
            it.toByteArray()
        })
    }
}

fun registerType(cls: Class<*>) {
    classMap[cls.name] = WeakReference(cls)
}

fun handleReceive(msg: ByteArray) {
    val event = object : ObjectInputStream(ByteArrayInputStream(msg)) {
        var eventClass: Class<*>? = null
        override fun resolveClass(desc: ObjectStreamClass): Class<*> {
            classMap[desc.name]?.get()?.let {
                eventClass = it
                return it
            }
            return eventClass?.classLoader?.loadClass(desc.name)
                ?: throw ClassNotFoundException(desc.name)
        }
    }.use { it.readObject() as RemoteEvent }
    launch { event.onReceive() }
}

val pubSub = object : BinaryJedisPubSub() {
    override fun onMessage(channel: ByteArray, message: ByteArray) {
        try {
            handleReceive(message)
        } catch (e: Throwable) {
            logger.log(Level.WARNING, "Fail to receive remote event", e)
        }
    }
}

onEnable {
    loop(Dispatchers.IO) {
        RedisApi.awaitInit()
        RedisApi.use {
            //blocking
            subscribe(pubSub, group.toByteArray())
        }
    }
}

onDisable {
    if (pubSub.isSubscribed)
        pubSub.unsubscribe()
}