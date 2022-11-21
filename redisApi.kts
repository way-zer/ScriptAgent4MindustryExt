@file:Import("redis.clients:jedis:4.3.1", mavenDepends = true)

package coreLibrary

import coreLibrary.lib.util.ServiceRegistry
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import java.util.logging.Level

@Suppress("unused")//Api
object Redis : ServiceRegistry<JedisPool>() {
    inline fun <T> use(body: Jedis.() -> T): T {
        return get().resource.use(body)
    }
}

val addr by config.key("redis://redis:6379", "redis地址", "重载生效")
onEnable {
    try {
        Redis.provide(this, JedisPool(addr).apply {
            resource.use { it.ping() }
        })
    } catch (e: Throwable) {
        logger.log(Level.WARNING, "连接Redis服务器失败: $addr", e)
        return@onEnable ScriptManager.disableScript(this, "连接Redis服务器失败: $e")
    }
}

onDisable {
    Redis.getOrNull()?.close()
}