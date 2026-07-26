@file:Suppress("MemberVisibilityCanBePrivate")

package wayzer

import arc.files.Fi
import arc.struct.StringMap
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.emitAsync
import cf.wayzer.scriptAgent.thisContextScript
import coreLibrary.lib.config
import coreLibrary.lib.with
import coreMindustry.lib.broadcast
import coreMindustry.lib.game
import coreMindustry.lib.nextTick
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mindustry.Vars
import mindustry.core.GameState
import mindustry.game.Rules
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.io.MapIO
import mindustry.io.SaveIO
import mindustry.maps.Map
import kotlin.time.Duration.Companion.seconds

typealias RuleModifier = Rules.() -> Unit

class MapChangeEvent(
    val info: MapInfo,
    val map: Map,
    val rules: Rules = map.applyRules(info.mode),
) :
    Event, Event.Cancellable {
    /** Should call other load*/
    override var cancelled: Boolean = false
    @Deprecated("modify rules directly", ReplaceWith("rules.block()"))
    fun modifyRule(block: RuleModifier) {
        rules.block()
    }

    companion object : Event.Handler()
}

object MapManager {
    var current: MapInfo =
        MapInfo(MapRegistry.SaveProvider, Vars.state.rules.idInTag, Vars.state.rules.mode(), Vars.state.map)
        private set
    internal var tmpVarSet: (() -> Unit)? = null

    @Suppress("unused")
    @Deprecated("old", level = DeprecationLevel.HIDDEN)
    fun loadMap(info: MapInfo? = null, isSave: Boolean = false) {
        loadMap(info)
    }

    fun loadMap(info: MapInfo? = null) {
        thisContextScript().launch(Dispatchers.game) {
            loadMapSync(info)
        }
    }

    suspend fun loadMapSync(info: MapInfo? = null): Boolean {
        @Suppress("NAME_SHADOWING") var info: MapInfo? = info
        try {
            info = info ?: MapRegistry.nextMapInfo()
            val map = info.loadMap().run {
                Map(file, width, height, StringMap(tags), custom, version, build) //copy tags
            }
            loadMapSync(info, map)
            return true
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            broadcast(
                "[red]加载地图地图{info.name}失败: {reason}".with(
                    "info" to (info ?: ""),
                    "reason" to (e.message ?: "")
                )
            )
            thisContextScript().launch(Dispatchers.game) {
                delay(1.seconds)
                loadMapSync()
            }
            return false
        }
    }

    suspend fun loadMapSync(info: MapInfo, map: Map) {
        val event = MapChangeEvent(info, map).apply {
            rules.idInTag = info.id
            Regex("\\[(@[a-zA-Z0-9]+)(=[^=\\]]+)?]").findAll(map.description()).forEach {
                val value = it.groupValues[2].takeIf(String::isNotEmpty) ?: "true"
                rules.tags.put(it.groupValues[1], value.removePrefix("="))
            }
        }
        if (event.emitAsync().cancelled) return

        thisContextScript().logger.info("loadMap $info")
        if (!Vars.net.server()) Vars.netServer.openServer()
        val players = Groups.player.toList()
        Call.worldDataBegin()
        Vars.logic.reset()
        //Hack: Some old tasks have posted, so we let they run.
        Vars.world.resize(0, 0)
        nextTick()

        current = info
        try {
            tmpVarSet = block@{
                if (map == MapRegistry.GeneratorMap) {
                    Vars.state.rules.idInTag = info.id
                    return@block
                }
                Vars.state.map = map
                Vars.state.rules = event.rules
            }
            info.provider.loadMap(info) // EventType.ResetEvent
            // do set state.rules when save versions < 13
            // EventType.DataPatchLoadEvent : emit when version >=11
            // do set state.rules when save versions >= 13
            // EventType.WorldLoadBeginEvent
            // EventType.WorldLoadEndEvent
            // EventType.WorldLoadEvent
            // Not generator: EventType.SaveLoadEvent
        } catch (e: Throwable) {
            tmpVarSet = null
            players.forEach { it.add() }
            throw e
        }


        if (info.provider == MapRegistry.SaveProvider) {
            Vars.state.set(GameState.State.playing)
        } else {
            Vars.logic.play() // EventType.PlayEvent
        }

        players.forEach {
            if (it.con == null) return@forEach
            it.admin.let { was ->
                it.reset()
                it.admin = was
            }
            it.team(Vars.netServer.assignTeam(it, players))
            Vars.netServer.sendWorldData(it)
        }
        players.forEach { it.add() }
    }

    fun loadSave(file: Fi) {
        val map = MapIO.createMap(file, true)
        loadMap(MapInfo(MapRegistry.SaveProvider, map.rules().idInTag, map.rules().mode(), map))
    }

    fun getSlot(id: Int): Fi? {
        val file = SaveIO.fileFor(id)
        if (!SaveIO.isSaveValid(file)) return null
        val voteFile = SaveIO.fileFor(configTempSaveSlot)
        if (voteFile.exists()) voteFile.delete()
        file.copyTo(voteFile)
        return voteFile
    }

    //private
    private val configTempSaveSlot by thisContextScript().config.key(111, "临时缓存的存档格位")

    /** Use for identity Save */
    private var Rules.idInTag: Int
        get() = tags.getInt("id", -1)
        set(value) {
            tags.put("id", value.toString())
        }
}