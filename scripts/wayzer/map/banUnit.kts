package wayzer.map

import mindustry.ctype.ContentType
import mindustry.type.UnitType

val list
    get() = state.rules.tags["@banUnit"].orEmpty()
        .split(';')
        .filterNot { it.isEmpty() }
        .map { content.getByName<UnitType>(ContentType.unit, it) }

listen<EventType.PlayEvent> {
    list.takeIf { it.isNotEmpty() }?.let { list ->
        broadcast("[red]本地图禁用单位:[yellow]{list}".with("list" to list))
    }
}

listen<EventType.PlayerJoin> { e ->
    list.takeIf { it.isNotEmpty() }?.let { list ->
        e.player.sendMessage("[red]本地图禁用单位:[yellow]{list}".with("list" to list))
    }
}

listen<EventType.UnitUnloadEvent> {
    if (it.unit.type in list) {
        Call.label("[red]本地图已禁用单位${it.unit.type.name}", 10f, it.unit.x, it.unit.y)
        it.unit.set(0f, 0f)
        it.unit.kill()
    }
}