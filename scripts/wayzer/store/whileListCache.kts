@file:Depends("wayzer/user/ext/whiteList")
@file:Depends("coreLibrary/extApi/KVStore", "储存记录")
@file:Depends("coreLibrary/extApi/rpcService", "RPC通讯实现Cache持久化和ip快速登录")

import coreLib.extApi.KVStore
import coreLib.extApi.RpcService
import coreLib.extApi.get
import coreLib.extApi.register
import org.h2.mvstore.type.StringDataType
import wayzer.user.ext.AuthCache


class CacheImpl(private val map: MutableMap<String, String>) : java.rmi.server.UnicastRemoteObject(), AuthCache {
    override fun get(uid: String, usid: String, ip: String): String? {
        val gid = map["$uid/$usid"] ?: map["IP/$uid/$ip"]
        if (gid != null) put(uid, usid, ip, gid)
        return gid
    }

    override fun put(uid: String, usid: String, ip: String, gid: String) {
        map["$uid/$usid"] = gid
        map["IP/$uid/$ip"] = gid
    }
}

class WithCache(private val cache: AuthCache, private val fallback: AuthCache) : AuthCache {
    override fun get(uid: String, usid: String, ip: String): String? {
        cache.get(uid, usid, ip)?.let { return it }
        return fallback.get(uid, usid, ip)?.also { cache.put(uid, usid, ip, it) }
    }

    override fun put(uid: String, usid: String, ip: String, gid: String) {
        cache.put(uid, usid, ip, gid)
        fallback.put(uid, usid, ip, gid)
    }
}

val localCache by autoInit {
    val map = Services.get<KVStore>().get().open("authCache", StringDataType.INSTANCE)
    CacheImpl(map) as AuthCache
}

onEnable {
    val rpcService = Services.get<RpcService>().get()
    rpcService.register { localCache }

    var store = rpcService.get<AuthCache>()
    if (!rpcService.isMaster) store = WithCache(localCache, store)
    Services.provide(localCache)
}