package wayzer.map

import mindustry.ctype.ContentType
import mindustry.type.UnitType

var list = emptySet<UnitType>()

listen<EventType.PlayEvent> {
    val old = state.rules.tags["@banUnit"].orEmpty()
        .split(';')
        .filterNot { it.isEmpty() }
        .mapNotNull { content.getByName<UnitType>(ContentType.unit, it) }
    list = state.rules.bannedUnits.toSet() + old
    if (list.isNotEmpty()) {
        state.rules.bannedUnits.addAll(*list.toTypedArray())
        broadcast("[red]本地图禁用单位:[yellow]{list}".with("list" to list.map { "${it.emoji()}${it.localizedName}" }))
    }
}

listen<EventType.PlayerJoin> { e ->
    if (list.isNotEmpty())
        e.player.sendMessage("[red]本地图禁用单位:[yellow]{list}".with("list" to list.map { "${it.emoji()}${it.localizedName}" }))
}

listen<EventType.UnitUnloadEvent> {
    val type = it.unit.type
    if (type in list) {
        Call.label("[red]本地图已禁用单位${type.emoji()}${type.localizedName}}", 10f, it.unit.x, it.unit.y)
        it.unit.set(0f, 0f)
        it.unit.kill()
    }
}