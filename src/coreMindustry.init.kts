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
    ConfigBuilder.init(Vars.dataDirectory.child("scriptsConfig.conf").file())
    DataStoreApi.open(Vars.dataDirectory.child("scriptAgent.db").absolutePath())
    ICommands.rootProvider.set(serverRootCommands)
    clientRootCommands.addSub(ICommands.controlCommand)
}

onDisable{
    DataStoreApi.close()
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