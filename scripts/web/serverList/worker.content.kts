import kotlinx.coroutines.launch
import web.lib.serverList.SharedData
import kotlin.concurrent.schedule

var serverList by config.key(emptySet<String>(), "服务器列表名单")

onEnable {
    SharedCoroutineScope.launch {
        serverList.forEach { SharedData.add(it) }
    }
    SharedTimer.schedule(0, 60 * 1000) {
        if (SharedData.servers.size > serverList.size)
            serverList = SharedData.servers.keys.toSet()
        SharedData.updateAll()
    }.let {
        onDisable {
            it.cancel()
        }
    }
}