@file:Depends("coreLibrary")
@file:Import("arc.Core", libraryByClass = true)
@file:Import("arc.Core", defaultImport = true)
@file:Import("mindustry.gen.Player", defaultImport = true)
@file:Import("mindustry.gen.Call", defaultImport = true)
@file:Import("mindustry.game.EventType", defaultImport = true)
@file:Import("mindustry.Vars.*", defaultImport = true)
@file:Import("coreMindustry.lib.*", defaultImport = true)

name = "Mindustry 核心脚本模块"

generateHelper()

fun updateOriginCommandHandler(client: arc.util.CommandHandler, server: arc.util.CommandHandler) {
    netServer?.apply {
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
    Commands.rootProvider.provide(this, RootCommands)
    updateOriginCommandHandler(
        MyCommandHandler("/", Config.clientCommands),
        MyCommandHandler("", Config.serverCommands)
    )
}

onDisable {
    updateOriginCommandHandler(Config.clientCommands, Config.serverCommands)
}