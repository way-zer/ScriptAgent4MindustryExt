@file:Depends("coreLibrary/extApi/redisApi", "基于redis")

package coreLibrary.extApi

import redis.clients.jedis.BinaryJedisPubSub
import java.io.*
import java.util.logging.Level

val group by config.key("_SA_RemoteEvent")

fun remoteEmit(event: RemoteEvent) = launch(Dispatchers.IO) {
    RedisApi.Redis.use {
        publish(group.toByteArray(), ByteArrayOutputStream().use {
            ObjectOutputStream(it).writeObject(event)
            it.toByteArray()
        })
    }
}

fun handleReceive(msg: ByteArray) {
    try {
        val event = object : ObjectInputStream(ByteArrayInputStream(msg)) {
            var eventClass: Class<*>? = null
            override fun resolveClass(desc: ObjectStreamClass): Class<*> {
                RemoteEvent.Impl.classMap[desc.name]?.get()?.let {
                    eventClass = it
                    return it
                }
                return eventClass?.classLoader?.loadClass(desc.name)
                    ?: throw ClassNotFoundException(desc.name)
            }
        }.use { it.readObject() as RemoteEvent }
        launch { event.onReceive() }
    } catch (e: Throwable) {
        logger.log(Level.WARNING, "Fail to receive remote event", e)
    }
}

onEnable {
    loop(Dispatchers.IO) {
        RedisApi.Redis.awaitInit()
        RedisApi.Redis.use {
            subscribe(
                object : BinaryJedisPubSub() {
                    init {
                        onDisable { unsubscribe() }
                    }

                    override fun onMessage(channel: ByteArray, message: ByteArray) {
                        handleReceive(message)
                    }
                }, group.toByteArray()
            )
        }
    }
}