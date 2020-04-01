package wayzer.lib

import arc.files.Fi
import mindustry.entities.type.Player
import mindustry.game.Gamemode
import mindustry.maps.Map

object SharedData {
    interface IMapManager{
        //base
        val maps:Array<Map>
        fun loadMap(map: Map = nextMap(), mode: Gamemode = bestMode(map))
        fun loadSave(file: Fi)
        fun nextMap(map: Map? = null, mode: Gamemode = Gamemode.survival): Map
        fun bestMode(map: Map): Gamemode
        //ext
        fun getSlot(id: Int):Fi?
    }
    interface IAdmin{
        fun isAdmin(player:Player):Boolean
        fun ban(player: Player,uuid:String)
        fun secureLog(tag:String,string: String)
    }
    lateinit var mapManager: IMapManager
    lateinit var admin: IAdmin
}