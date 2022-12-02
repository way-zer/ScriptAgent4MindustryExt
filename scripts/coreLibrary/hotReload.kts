package coreLibrary

import cf.wayzer.placehold.PlaceHoldApi.with
import cf.wayzer.scriptAgent.registry.DirScriptRegistry
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.Commands
import java.nio.file.*

var watcher: WatchService? = null

fun enableWatch() {
    if (watcher != null) return//Enabled
    watcher = FileSystems.getDefault().newWatchService()
    Config.rootDir.walkTopDown().onEnter { it.name != "cache" && it.name != "lib" && it.name != "res" }
        .filter { it.isDirectory }.forEach {
            it.toPath().register(watcher!!, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY)
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
                    file.toString().endsWith(Config.contentScriptSuffix) -> { //处理子脚本
                        val id = DirScriptRegistry.getIdByFile(file.toFile(), Config.rootDir)
                        val script = ScriptRegistry.findScriptInfo(id) ?: return@forEach
                        logger.info("脚本文件更新: ${event.kind().name()} ${script.id}")
                        delay(1000)
                        val state = script.scriptState
                        when {
                            state == ScriptState.Found -> logger.info("  新脚本: 请使用sa load加载")
                            else -> {
                                val oldEnable = state == ScriptState.ToEnable || state.enabled
                                ScriptManager.transaction {
                                    add(script)
                                    unload(addAllAffect = true)
                                    load()
                                    if (oldEnable) enable()
                                }
                                logger.info(if (oldEnable) "  新脚本启用成功" else "  新脚本加载成功: 请使用sa enable启用")
                            }
                        }
                    }

                    file.toFile().isDirectory -> {//添加子目录到Watch
                        file.register(
                            watcher!!,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY
                        )
                    }
                }
            }
            if (!key.reset()) return@launch
        }
    }
}

onEnable {
    Commands.controlCommand += CommandInfo(this, "hotReload", "开关脚本自动热重载") {
        permission = "scriptAgent.control.hotReload"
        body {
            if (watcher == null) {
                enableWatch()
                reply("[green]脚本自动热重载监测启动".with())
            } else {
                watcher?.close()
                watcher = null
                reply("[yellow]脚本自动热重载监测关闭".with())
            }
        }
    }
    onDisable { Commands.controlCommand.removeAll(this) }
}

onDisable {
    withContext(Dispatchers.IO) {
        watcher?.close()
    }
}