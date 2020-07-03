import cf.wayzer.placehold.PlaceHoldApi.with
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.IInitScript
import cf.wayzer.script_agent.ScriptManager
import java.nio.file.*

var watcher: WatchService? = null

fun enableWatch() {
    if (watcher != null) return//Enabled
    watcher = FileSystems.getDefault().newWatchService()
    Config.rootDir.walkTopDown().onEnter { it.name != "cache" && it.name != "lib" && it.name != "res" }
            .filter { it.isDirectory }.forEach {
                it.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
            }
    launch(Dispatchers.IO) {
        while (true) {
            val key = try {
                watcher?.take() ?: return@launch
            } catch (e: ClosedWatchServiceException) {
                return@launch
            }
            key.pollEvents().forEach { event ->
                if (event.count() != 1) return@forEach
                val file = (key.watchable() as Path).resolve(event.context() as? Path ?: return@forEach)
                when {
                    file.fileName.endsWith(Config.moduleDefineSuffix) -> //处理模块重载
                        ScriptManager.loadModule(file.toFile(), force = true, enable = true)
                    file.fileName.endsWith(Config.contentScriptSuffix) -> { //处理子脚本重载
                        val module = Config.findModuleBySource(file.toFile())?.let {
                            ScriptManager.getScript(it) as? IInitScript
                        } ?: return@forEach println("[WARN]Can't get Module by $file")
                        ScriptManager.loadContent(module, file.toFile(), force = true, enable = true)
                    }
                    file.toFile().isDirectory -> {//添加子目录到Watch
                        file.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
                    }
                }
            }
            if (!key.reset()) return@launch
        }
    }
}

ICommands.controlCommand.addSub(ICommand(this, "hotReload", "开关脚本自动热重载") {
    if (!hasPermission("scriptAgent.control.hotReload")) return@ICommand sendMessage("[red]你没有权限使用该命令".with())
    if (watcher == null) {
        enableWatch()
        sendMessage("[green]脚本自动热重载监测启动".with())
    } else {
        watcher?.close()
        watcher = null
        sendMessage("[yellow]脚本自动热重载监测关闭".with())
    }
})

onDisable {
    watcher?.close()
}