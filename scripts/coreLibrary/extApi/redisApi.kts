@file:Import("redis.clients:jedis:4.4.3", mavenDepends = true)
@file:Import("./lib/RedisApi.kt", sourceFile = true)

import cf.wayzer.scriptAgent.util.Services
import redis.clients.jedis.JedisPool
import java.util.logging.Level

val addr by config.key("", "redis地址", "格式：\"redis://redis:6379\"，重载生效")
onEnable {
    if (addr.isEmpty()) {
        ScriptManager.disableScript(this, "未配置Redis服务器")
        return@onEnable
    }
    try {
        Services.provide(JedisPool(addr).apply {
            testOnCreate = true
            testOnBorrow = true
            resource.use { it.ping() }
            onDisable { close() }
        })
    } catch (e: Throwable) {
        logger.log(Level.WARNING, "连接Redis服务器失败: $addr", e)
        return@onEnable ScriptManager.disableScript(this, "连接Redis服务器失败: $e")
    }
}
