@file:DependsModule("coreLibrary")

import arc.Core
import coreMindustry.lib.*
import mindustry.Vars

name = "Mindustry 核心脚本模块"

addLibraryByClass("mindustry.Vars")
addDefaultImport("arc.Core")
addDefaultImport("mindustry.gen.Player")
addDefaultImport("mindustry.game.EventType")
addDefaultImport("mindustry.Vars.*")
addDefaultImport("coreMindustry.lib.*")
addDefaultImport("coreMindustry.lib.compatibilities.*")
generateHelper()

fun updateOriginCommandHandler(client: arc.util.CommandHandler, server: arc.util.CommandHandler) {
    Vars.netServer?.apply {
        javaClass.getDeclaredField("clientCommands").let {
            it.isAccessible = true
            it.set(this, client)
        }
    }
    Core.app.listeners.find { it.javaClass.simpleName == "ServerControl" }?.let {
        it.javaClass.getDeclaredField("handler").apply {
            isAccessible = true
            set(it, server)
        }
    }
}

Listener//ensure init

onEnable {
    Vars.dataDirectory.child("scriptsConfig.conf").file().takeIf { it.exists() }?.apply {
        println("检测到旧配置文件,自动迁移")
        copyTo(Config.dataDirectory.resolve("config.conf"), true)
        ConfigBuilder.reloadFile()
        delete()
    }
    Vars.dataDirectory.child("scriptAgent.db").file().takeIf { it.exists() }?.let {
        println("检测到旧数据储存文件,已弃用，请手动移除 $it")
    }
    Commands.rootProvider.set(RootCommands)
    updateOriginCommandHandler(
        MyCommandHandler("/", Config.clientCommands),
        MyCommandHandler("", Config.serverCommands)
    )
}

onDisable {
    updateOriginCommandHandler(Config.clientCommands, Config.serverCommands)
}