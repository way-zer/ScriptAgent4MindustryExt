import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import web.lib.serverList.SharedData

var serverList by config.key(emptySet<String>(), "服务器列表名单")

onEnable {
    launch(Dispatchers.IO) {
        serverList.forEach {
            val ok = kotlin.runCatching {
                SharedData.add(it) == "OK"
            }.getOrDefault(false)
            if (!ok)
                println("[$clsName]Ping服务器失败,已移除: $it")
        }
        serverList = SharedData.servers.keys.toSet()
        println("[$clsName]成功加载${serverList.size}个服务器")
    }
    launch {
        delay(60_000)
        if (SharedData.servers.size > serverList.size)
            serverList = SharedData.servers.keys.toSet()
        SharedData.updateAll(this)
    }
}