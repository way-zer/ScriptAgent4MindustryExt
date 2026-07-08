@file:Implement(PosMark.Service::class)

package mapScript.shared

import mindustry.world.Tile

fun parse(tile: Tile): List<PosMark>? {
    if (tile.block() != Blocks.worldMessage) return null
    val lines = tile.build.config().toString().lines()
    if (lines.any { it.firstOrNull() != '@' }) return null
    val team = tile.team()
    return lines.map { line ->
        val sp = line.split(" ").filter { it.isNotBlank() }
        PosMark(
            sp[0], tile, team,
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

/*override*/ fun getPoses(type: String) = posMap[type].orEmpty()

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
val commands = mutableMapOf<String, (pos: PosMark) -> Unit>()
onDisable { commands.clear() }
/*override*/ fun registerCommand(type: String, handler: (pos: PosMark) -> Unit) {
    commands[type] = handler
}