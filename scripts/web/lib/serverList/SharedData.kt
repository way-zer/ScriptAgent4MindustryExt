package web.lib.serverList

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SharedData {
    val servers = mutableMapOf<String, PingUtil.Info>()//address->info
    fun add(addressWithPort: String): String {
        if (servers.containsKey(addressWithPort.removeSuffix(":6567"))) error("Replicate server")
        val info = PingUtil.ping(addressWithPort)
        if (servers.values.find { it.name == info.name } != null) {
            return "服务器已存在"
        }
        servers[info.address] = info
        return "OK"
    }


    fun updateAll(scope: CoroutineScope) {
        servers.values.forEach {
            scope.launch(Dispatchers.IO) {
                try {
                    servers[it.address] = PingUtil.ping(it.address)
                } catch (e: Throwable) {
                    it.apply {
                        online = false
                        lastUpdate = System.currentTimeMillis()
                    }
                }
            }
        }
    }
}