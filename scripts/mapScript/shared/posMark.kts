package mapScript.shared

import mindustry.game.Team
import mindustry.world.Tile

//定义一种标记格式
//放置世界信息版，每行可设置一个标记，形如
// @zone w=5 h=5
//地图加载后，将存储在@zone类型信息中

data class Pos(val type: String, val tile: Tile, val team: Team, val arg: Map<String, String>) {
    fun error(msg: String, duration: Float = 10f) {
        Call.labelReliable(msg, duration, tile.worldx(), tile.worldy())
    }
}

fun parse(tile: Tile): List<Pos>? {
    if (tile.block() != Blocks.worldMessage) return null
    val lines = tile.build.config().toString().lines()
    if (lines.any { it.firstOrNull() != '@' }) return null
    val team = tile.team()
    return lines.map { line ->
        val sp = line.split(" ").filter { it.isNotBlank() }
        Pos(sp[0], tile, team,
            sp.drop(1).associate { it.substringBefore('=') to it.substringAfter('=', "") })
    }
}

//静态地图标记

val posMap by autoInit {
    val all = buildList {
        world.tiles.forEach {
            val parsed = parse(it) ?: return@forEach
            it.remove()
            addAll(parsed)
        }
    }
    all.groupBy { it.type }
}

fun getPoses(type: String) = posMap[type].orEmpty()

//动态地图标记
listen<EventType.TileChangeEvent> { event ->
    val tile = event.tile
    if (tile.block() != Blocks.worldMessage) return@listen
    launch(Dispatchers.gamePost) {
        val parsed = parse(tile) ?: return@launch
        tile.removeNet()
        parsed.forEach {
            commands[it.type]?.invoke(it)
        }
    }
}
val commands = mutableMapOf<String, (pos: Pos) -> Unit>()
onDisable { commands.clear() }
fun registerCommand(type: String, handler: (pos: Pos) -> Unit) {
    commands[type] = handler
}