package mapScript.lib

import arc.Events
import arc.struct.Seq
import arc.struct.StringMap
import arc.util.Log
import cf.wayzer.placehold.VarString
import cf.wayzer.scriptAgent.ScriptManager
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.thisContextScript
import coreMindustry.lib.MindustryDispatcher
import mindustry.Vars
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.game.Rules
import mindustry.io.JsonIO
import mindustry.maps.Map
import mindustry.world.Tiles
import wayzer.MapInfo
import wayzer.MapManager
import wayzer.MapProvider
import java.util.logging.Level
import kotlin.system.measureTimeMillis

class ScriptMapGenerator(val script: Script, val width: Int, val height: Int) {
    private val genRounds = LinkedHashMap<String, (tiles: Tiles) -> Unit>()
    val rules = Rules()

    init {
        genRound("init") { it.fill() }
    }

    fun genRound(name: String, body: (tiles: Tiles) -> Unit) {
        genRounds[name] = body
    }

    fun load() {
        try {
            //Load patches, MDT don't do this with loadGenerator
            val patches = Seq<String>()
            Events.fire(EventType.ContentPatchLoadEvent(patches))
            if (!patches.isEmpty) {
                try {
                    Vars.state.patcher.apply(patches)
                } catch (e: Throwable) {
                    Log.err("Failed to apply patches: $patches", e)
                }
            }

            Vars.world.loadGenerator(width, height) { tiles ->
                genRounds.forEach { (name, round) ->
                    val time = measureTimeMillis { round.invoke(tiles) }
                    script.logger.info("Generate $name costs $time ms.")
                }
            }
        } catch (e: Throwable) {
            script.logger.log(Level.SEVERE, "loadGenerator出错", e)
            MapManager.loadMap()
        }
        if (script.enabled) return// Already enabled inside WorldLoadEvent
        MindustryDispatcher.safeBlocking {
            ScriptManager.enableScript(script, true)
        }
        if (!thisContextScript().checkEnabled(script.scriptInfo)) {
            MapManager.loadMap()
        }
    }

    data class Info(val info: MapInfo, val filters: Set<String>, val generator: ScriptMapGenerator)

    object Provider : MapProvider() {
        val knownMaps = mutableMapOf<Int, Info>()

        override suspend fun searchMaps(search: String?) = knownMaps.values
            .filter { search == null || search in it.filters }
            .map { it.info }

        override suspend fun findById(id: Int, reply: ((VarString) -> Unit)?): MapInfo? {
            return knownMaps[id]?.info
        }

        override suspend fun lazyGetMap(info: MapInfo): Map {
            val generator = knownMaps[info.id]!!.generator
            return Map(
                Vars.customMapDirectory.child("unknown"), generator.width, generator.height,
                StringMap().apply {
                    put("name", info.name)
                    put("author", info.author)
                    put("description", info.description)
                    put("rules", JsonIO.write(generator.rules))
                }, true
            )
        }

        override fun toString(): String = "ScriptGeneratorMap"

        override suspend fun loadMap(info: MapInfo) {
            val generator = knownMaps[info.id]?.generator ?: return MapManager.loadMap()
            generator.load()
        }
    }
}

@ScriptDsl
fun Script.registerGenerator(
    name: String,
    author: String,
    description: String,
    mode: Gamemode = Gamemode.survival,
    filter: Set<String> = setOf("all", "display", "special"),
    width: Int,
    height: Int,
    body: ScriptMapGenerator.() -> Unit
) {
    val mapId = id.split('/').last().toIntOrNull() ?: error("MapScript must named as {id}.kts")
    val info = MapInfo(
        ScriptMapGenerator.Provider, mapId, mode, meta = mapOf(
            "name" to name,
            "author" to author,
            "description" to description
        )
    )
    val generator = ScriptMapGenerator(this, width, height).apply(body)
    ScriptMapGenerator.Provider.knownMaps[mapId] = ScriptMapGenerator.Info(info, filter, generator)
    onUnload {
        ScriptMapGenerator.Provider.knownMaps.remove(mapId)
    }
}