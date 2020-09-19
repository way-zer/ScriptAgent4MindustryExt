//WayZer 版权所有(请勿删除版权注解)
package main

import arc.util.Time
import cf.wayzer.placehold.DynamicVar
import mindustry.entities.type.Player
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.maps.Map

name = "基础: 全局变量"

//SystemVars
registerVar("fps", "服务器fps", DynamicVar<Int> {
    (60f / Time.delta).toInt()
})
registerVar("heapUse", "内存占用(MB)", DynamicVar<Long> {
    Core.app.javaHeap / 1024 / 1024  //MB
})
//GameVars
registerVar("map", "当前游戏中的地图", DynamicVar<Map> {
    world.map
})
registerVarForType<Map>("地图的基础属性").apply {
    registerChild("name", DynamicVar { it, _ -> it.name() })
//        registerChild("id",DynamicVar{it,_->it.name()})
    registerChild("desc", DynamicVar { it, _ -> it.description() })
    registerChild("author", DynamicVar { it, _ -> it.author() })
    registerChild("width", DynamicVar { it, _ -> it.width })
    registerChild("height", DynamicVar { it, _ -> it.height })
    registerChild("fileName", DynamicVar { it, _ -> it.file?.nameWithoutExtension() })
}
registerVar("state.allUnit", "总单位数量", DynamicVar<Int> { Groups.unit.size() })
registerVar("state.allBan", "总禁封人数", DynamicVar<Int> { netServer.admins.banned.size })
registerVar("state.playerSize", "当前玩家数量", DynamicVar<Int> { playerGroup.size() })
registerVar("state.wave", "当前波数", DynamicVar<Int> { state.wave })
registerVar("state.enemies", "当前敌人数量", DynamicVar<Int> { state.enemies })

//PlayerVars
registerVarForType<Player>("玩家基础信息").apply {
    registerChild("name", DynamicVar { it, _ -> it.name })
    registerChild("uuid", DynamicVar { it, _ -> it.uuid })
    registerChild("ip", DynamicVar { it, _ -> it.con?.address })
    registerChild("team", DynamicVar { it, _ -> it.team() })
}
registerVar("team", "当前玩家的队伍", DynamicVar<Any> { getVar("player.team") })
registerVarForType<Team>("队伍的基础信息").apply {
    registerChild("name", DynamicVar { it, _ -> it.name })
    registerChild("color", DynamicVar { it, _ -> it.let { "[#${it.color}]" } })
    registerChild("colorizeName", DynamicVar { it, _ -> typeResolve(it, "color")?.toString() + typeResolve(it, "name")?.toString() })
}