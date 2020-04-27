package web.lib.serverList

import coreLibrary.lib.SharedCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object SharedData {
    val servers = mutableMapOf<String, PingUtil.Info>()//address->info
    fun add(addressWithPort: String) {
        if (servers.containsKey(addressWithPort.removeSuffix(":6567"))) error("Replicate server")
        val info = PingUtil.ping(addressWithPort)
        servers[info.address] = info
    }


    fun updateAll() {
        servers.values.forEach {
            SharedCoroutineScope.launch(Dispatchers.IO) {
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