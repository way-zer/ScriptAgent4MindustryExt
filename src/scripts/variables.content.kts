//WayZer 版权所有(请勿删除版权注解)
import arc.util.Time
import cf.wayzer.placehold.DynamicResolver
import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.placehold.types.DateResolver
import mindustry.entities.type.Player
import mindustry.game.Team
import mindustry.maps.Map
import java.util.*

name.set("基础: 全局变量")

class MapVars(private val map:Map) : DynamicVar<Map>(){
    override fun resolveThis(context: PlaceHoldContext): Map? {
        return map
    }
    init {
        registerChild("name"){it?.name()}
//        registerChild("id"){it?.name()}
        registerChild("desc"){it?.description()}
        registerChild("author"){it?.author()}
        registerChild("width"){it?.width}
        registerChild("height"){it?.height}
        registerChild("fileName"){it?.file?.nameWithoutExtension()}
    }
}
class PlayerVars(private val player:Player): DynamicVar<Player>(){
    override fun resolveThis(context: PlaceHoldContext): Player? {
        return player
    }
    init {
        registerChild("name"){it?.name}
        registerChild("uuid"){it?.uuid}
        registerChild("ip"){it?.con?.address}
//        registerChild("_info"){it?.info}
        registerChild("_team"){it?.team}
    }
}
class TeamVars(private val team:Team): DynamicVar<Team>(){
    override fun resolveThis(context: PlaceHoldContext): Team? {
        return team
    }
    init {
        registerChild("name"){it?.name}
        registerChild("color"){it?.let { "[#${it.color}]" }}
        registerChild("colorizeName") {  resolveChild(this,"color")?.toString()+ resolveChild(this,"name")?.toString() }
    }
}

registerTypeVar(Date::class.java){DateResolver(it as Date)}
registerTypeVar(Map::class.java){MapVars(it as Map)}
registerTypeVar(Player::class.java){PlayerVars(it as Player)}
registerTypeVar(Team::class.java){TeamVars(it as Team)}
//SystemVars
registerVar("fps",DynamicResolver.new {
    (60f / Time.delta()).toInt()
})
registerVar("heapUse",DynamicResolver.new {
    Core.app.javaHeap / 1024 / 1024  //MB
})

//GameVars
registerVar("map",DynamicResolver.new { world.map })
registerVar("state.allUnit",DynamicResolver.new { unitGroup.size() })
registerVar("state.allBan",DynamicResolver.new { netServer.admins.banned.size })
registerVar("state.playerSize",DynamicResolver.new { playerGroup.size() })
//PlayerVars
registerVar("team",DynamicResolver.new { getVar("player._team") })