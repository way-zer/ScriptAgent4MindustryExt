package wayzer.services

import arc.files.Fi
import cf.wayzer.scriptAgent.define.annotations.ServiceDefine
import mindustry.game.Gamemode
import mindustry.maps.Map

@ServiceDefine
interface MapService {
    //base
    val maps: Array<Map>
    fun loadMap(map: Map = nextMap(), mode: Gamemode = bestMode(map))
    fun loadSave(file: Fi)
    fun nextMap(map: Map? = null, mode: Gamemode = Gamemode.survival): Map
    fun bestMode(map: Map): Gamemode

    //ext
    fun getSlot(id: Int): Fi?
    fun setNextMap(map: Map)
}