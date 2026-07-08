package mapScript.lib

import mindustry.game.Team
import mindustry.gen.Call
import mindustry.world.Tile

//定义一种标记格式
//放置世界信息版，每行可设置一个标记，形如
// @zone w=5 h=5
//地图加载后，将存储在@zone类型信息中

data class PosMark(val type: String, val tile: Tile, val team: Team, val arg: Map<String, String>) {
    fun error(msg: String, duration: Float = 10f) {
        Call.labelReliable(msg, duration, tile.worldx(), tile.worldy())
    }

    //@file:Depends("mapScript/shared/PosMark")
    interface Service {
        fun getPoses(type: String): List<PosMark>
        fun registerCommand(type: String, handler: (PosMark) -> Unit)
    }
}