package coreMindustry.util

import arc.math.geom.QuadTree
import cf.wayzer.scriptAgent.define.Script
import coreMindustry.lib.listen
import mindustry.game.EventType
import mindustry.gen.Building

interface BuildingTracker<B : Building> {
    fun onAdd(building: B)
    fun onRemove(building: B)
}

inline fun <reified B : Building> Script.trackBuilding(
    tracker: BuildingTracker<B>,
    crossinline filter: (B) -> Boolean = { true }
) {
    listen<EventType.TilePreChangeEvent> {
        val build = it.tile.build
        if (build?.tile == it.tile && build is B && filter(build))
            tracker.onRemove(build)
    }
    listen<EventType.TileChangeEvent> {
        val build = it.tile.build
        if (build?.tile == it.tile && build is B && filter(build))
            tracker.onAdd(build)
    }
}

fun <B : Building> QuadTree<B>.asTracker() = object : BuildingTracker<B> {
    override fun onAdd(building: B) = insert(building)
    override fun onRemove(building: B) {
        remove(building)
    }
}