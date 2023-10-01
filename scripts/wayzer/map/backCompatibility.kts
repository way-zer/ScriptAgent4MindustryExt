@file:Depends("wayzer/maps")

package wayzer.map

import mindustry.ctype.ContentType
import mindustry.type.UnitType
import wayzer.MapChangeEvent
import java.time.Instant
import java.util.*

listenTo<MapChangeEvent>(Event.Priority.Before) {
    val oldBanUnit = rules.tags["@banUnit"].orEmpty()
        .split(';')
        .filterNot { it.isEmpty() }
        .mapNotNull { content.getByName<UnitType>(ContentType.unit, it) }
    if (oldBanUnit.isNotEmpty()) rules.bannedUnits.addAll(*oldBanUnit.toTypedArray())
    val time = info.map.tag("saved")?.toLongOrNull()?.let { Instant.ofEpochMilli(it) } ?: return@listenTo

    if (time < Calendar.getInstance().apply { set(2020, 3 - 1, 3) }.toInstant()) {
        return@listenTo //too old, may no pvp protect
    }
    if (time < Calendar.getInstance().apply { set(2022, 3 - 1, 3) }.toInstant()) {
        rules.tags.put("@banTeam", "2")
    }
}