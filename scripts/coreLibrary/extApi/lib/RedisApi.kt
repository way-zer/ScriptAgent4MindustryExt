package coreLib.extApi

import cf.wayzer.scriptAgent.util.Services
import coreLibrary.lib.get
import kotlinx.coroutines.flow.first
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

// @file:Depends("coreLibrary/extApi/redisApi")
object RedisApi {
    val service = Services.get<JedisPool>()
    fun get() = service.get()

    suspend fun awaitInit() {
        service.observe().first { it.isNotEmpty() }
    }

    inline fun <T> use(body: Jedis.() -> T): T {
        return get().resource.use(body)
    }
}