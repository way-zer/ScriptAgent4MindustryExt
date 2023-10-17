package mapScript.lib

import arc.struct.StringMap
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.ScriptRegistry
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.define.ScriptState
import cf.wayzer.scriptAgent.events.ScriptStateChangeEvent
import cf.wayzer.scriptAgent.listenTo
import cf.wayzer.scriptAgent.thisContextScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.PlaceHoldString
import mindustry.Vars
import mindustry.game.Gamemode
import mindustry.game.Rules
import mindustry.io.JsonIO
import mindustry.maps.Map
import mindustry.world.Tiles
import wayzer.MapInfo
import wayzer.MapManager
import wayzer.MapProvider
import wayzer.MapRegistry
import java.util.logging.Level
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.system.measureTimeMillis

object GeneratorSupport {
    private val knownMaps = mutableMapOf<Int, Pair<MapInfo, Set<String>>>()

    fun checkScript(script: Script) {
        val id = script.id.removePrefix("mapScript/").toIntOrNull() ?: return
        knownMaps.remove(id)

        val map = script.mapInfo ?: return
        val info = MapInfo(id, map, script.mapMode, load = { loadMap(script.id) })
        knownMaps[id] = info to script.mapFilters
    }

    fun loadMap(scriptId: String) {
        Vars.logic.reset()
        val script = findAndLoadScript(scriptId)?.inst ?: return MapManager.loadMap()
        val map = script.mapInfo ?: return MapManager.loadMap()
        try {
            Vars.world.loadGenerator(map.width, map.height) { tiles ->
                script.genRound.forEach { (name, round) ->
                    val time = measureTimeMillis { round(tiles) }
                    script.logger.info("Do $name costs $time ms.")
                }
            }
        } catch (e: Throwable) {
            script.logger.log(Level.SEVERE, "loadGenerator出错", e)
            MapManager.loadMap()
        }
    }

    private fun Script.init() {
        val moduleId = id
        listenTo<ScriptStateChangeEvent>(Event.Priority.Watch) {
            if (next == ScriptState.Loaded && script.id.startsWith(moduleId)) {
                script.inst?.let(GeneratorSupport::checkScript)
            }
        }
        onEnable {
            ScriptRegistry.allScripts { it.scriptState.loaded && it.id.startsWith(moduleId) }
                .forEach { it.inst?.let(GeneratorSupport::checkScript) }
        }
        MapRegistry.register(this, object : MapProvider() {
            override suspend fun searchMaps(search: String?) = knownMaps.values
                .filter { search == null || search in it.second }
                .map { it.first }

            override suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)?): MapInfo? {
                return knownMaps[id]?.first
            }
        })
    }

    init {
        thisContextScript().init()
    }
}

var Script.mapInfo by DSLBuilder.dataKey<Map>()
var Script.mapFilters by DSLBuilder.dataKeyWithDefault { setOf("all", "display", "special") }
var Script.mapMode by DSLBuilder.dataKeyWithDefault { Gamemode.survival }

@ScriptDsl
val Script.genRound by DSLBuilder.dataKeyWithDefault {
    mutableListOf<Pair<String, (tiles: Tiles) -> Unit>>("init" to { it.fill() })
}

@OptIn(ExperimentalContracts::class)
@ScriptDsl
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