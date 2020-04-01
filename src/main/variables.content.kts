//WayZer 版权所有(请勿删除版权注解)
package main

import arc.util.Time
import cf.wayzer.placehold.DynamicVar
import mindustry.entities.type.Player
import mindustry.game.Team
import mindustry.maps.Map

name="基础: 全局变量"

//SystemVars
PlaceHoldApi.registerGlobalDynamicVar("fps") { _, _ ->
    (60f / Time.delta()).toInt()
}
PlaceHoldApi.registerGlobalDynamicVar("heapUse") { _, _ ->
    Core.app.javaHeap / 1024 / 1024  //MB
}

//GameVars
PlaceHoldApi.registerGlobalDynamicVar("map") { _, _ -> world.map }
PlaceHoldApi.typeBinder<Map>().apply {
    registerChild("name", DynamicVar { it, _ -> it.name() })
//        registerChild("id",DynamicVar{it,_->it.name()})
    registerChild("desc", DynamicVar { it, _ -> it.description() })
    registerChild("author", DynamicVar { it, _ -> it.author() })
    registerChild("width", DynamicVar { it, _ -> it.width })
    registerChild("height", DynamicVar { it, _ -> it.height })
    registerChild("fileName", DynamicVar { it, _ -> it.file?.nameWithoutExtension() })
}
PlaceHoldApi.registerGlobalDynamicVar("state.allUnit") { _, _ -> unitGroup.size() }
PlaceHoldApi.registerGlobalDynamicVar("state.allBan") { _, _ -> netServer.admins.banned.size }
PlaceHoldApi.registerGlobalDynamicVar("state.playerSize") { _, _ -> playerGroup.size() }
PlaceHoldApi.registerGlobalDynamicVar("state.wave") { _, _ -> state.wave }
PlaceHoldApi.registerGlobalDynamicVar("state.enemies") { _, _ -> state.enemies }

//PlayerVars
PlaceHoldApi.typeBinder<Player>().apply {
    registerChild("name", DynamicVar { it, _ -> it.name })
    registerChild("uuid", DynamicVar { it, _ -> it.uuid })
    registerChild("ip", DynamicVar { it, _ -> it.con?.address })
//        registerChild("_info",DynamicVar{it,_->it.info})
    registerChild("team", DynamicVar { it, _ -> it.team })
}
PlaceHoldApi.registerGlobalDynamicVar("team") { _, _ -> getVar("player.team") }
PlaceHoldApi.typeBinder<Team>().apply {
    registerChild("name", DynamicVar { it, _ -> it.name })
    registerChild("color", DynamicVar { it, _ -> it.let { "[#${it.color}]" } })
    registerChild("colorizeName", DynamicVar { it, _ -> typeResolve(it, "color")?.toString() + typeResolve(it, "name")?.toString() })
}