@file:DependsModule("coreLibrary")

import arc.Events
import arc.func.Cons
import arc.struct.Array
import arc.struct.ObjectMap
import arc.util.Log
import cf.wayzer.script_agent.Config
import coreMindustry.lib.*
import coreMindustry.lib.ContentExt.allCommands
import coreMindustry.lib.ContentExt.listener
import mindustry.Vars

name = "Mindustry 核心脚本模块"

addLibraryByClass("mindustry.Vars")
addDefaultImport("arc.Core")
addDefaultImport("mindustry.Vars.*")
addDefaultImport("coreMindustry.lib.*")
generateHelper()

onEnable{
    Vars.dataDirectory.child("scriptsConfig.conf").file().takeIf { it.exists() }?.apply {
        println("检测到旧配置文件,自动迁移")
        copyTo(Config.dataDirectory.resolve("config.conf"),true)
        delete()
    }
    Vars.dataDirectory.child("scriptAgent.db").file().takeIf { it.exists() }?.let {
        println("检测到旧数据储存文件,已弃用，请手动移除 $it")
    }
    Commands.rootProvider.set(serverRootCommands)
    clientRootCommands.addSub(CommandInfo(null, "ScriptAgent", "ScriptAgent 控制指令", {
        aliases = listOf("sa")
        permission = "scriptAgent.admin"
    },Commands.controlCommand))
}

onAfterContentEnable{child->
    child.allCommands.forEach { with(it) {
        if(type.client()){
            Config.clientCommands.removeCommand(name)
            Config.clientCommands.register(name,param,description,runner)
        }
        if(type.server()){
            Config.serverCommands.removeCommand(name)
            Config.serverCommands.register(name,param,description,runner)
        }
    }}
    child.listener.forEach{
        fun <T : Any> ContentExt.Listener<T>.listen(){ Events.on(cls,this)}
        it.listen()
    }
}

onBeforeContentDisable{child->
    @Suppress("UNCHECKED_CAST")
    val events = Events::class.java.getDeclaredField("events").apply { isAccessible =true }.get(null) as? ObjectMap<Any, Array<Cons<*>>>
    child.listener.forEach{
        events?.get(it.cls)?.remove(it)?: Log.warn("Can't unregister event")
    }
    child.allCommands.forEach { with(it) {
        if(type.client())
            Config.clientCommands.removeCommand(name)
        if(type.server())
            Config.serverCommands.removeCommand(name)
    }}
}