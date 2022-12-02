package mapScript.lib

import arc.struct.StringMap
import cf.wayzer.scriptAgent.contextScript
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.thisContextScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import mindustry.Vars
import mindustry.game.Gamemode
import mindustry.game.Rules
import mindustry.io.JsonIO
import mindustry.maps.Map
import mindustry.world.Tiles
import wayzer.MapInfo
import wayzer.MapManager
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

object GeneratorSupport {
    val knownMaps = mutableMapOf<Int, Pair<MapInfo, Set<String>>>()

    /** use for MapProvider.findById*/
    fun findGenerator(id: Int): MapInfo? {
        return knownMaps[id]?.first
    }

    fun checkScript(script: Script) {
        val id = script.id.removePrefix("mapScript/").toIntOrNull() ?: return
        knownMaps.remove(id)

        val map = script.mapInfo ?: return
        val info = MapInfo(id, map, script.mapMode, load = { loadMap(script.id) })
        knownMaps[id] = info to script.mapFilters
        thisContextScript().logger.info("Find generator script: ${script.id}")
    }

    fun loadMap(scriptId: String) {
        Vars.logic.reset()

        if (!contextScript<mapScript.Module>().loadMapScript(scriptId)) {
            MapManager.loadMap()
        }
    }
}

var Script.mapInfo by DSLBuilder.dataKey<Map>()
var Script.mapFilters by DSLBuilder.dataKeyWithDefault { setOf("all", "display", "special") }
var Script.mapMode by DSLBuilder.dataKeyWithDefault { Gamemode.survival }
val Script.genRound by DSLBuilder.dataKeyWithDefault {
    mutableListOf<Pair<String, (tiles: Tiles) -> Unit>>("init" to { it.fill() })
}

@OptIn(ExperimentalContracts::class)
inline fun Script.setMapInfo(width: Int, height: Int, tagsApply: StringMap.() -> Unit, rulesApply: Rules.() -> Unit) {
    contract {
        callsInPlace(tagsApply, InvocationKind.EXACTLY_ONCE)
        callsInPlace(rulesApply, InvocationKind.EXACTLY_ONCE)
    }
    val rules = Rules().apply(rulesApply)
    val tags = StringMap().apply(tagsApply)
    tags.put("rules", JsonIO.write(rules))
    mapInfo = Map(Vars.customMapDirectory.child("unknown"), width, height, tags, true)
}