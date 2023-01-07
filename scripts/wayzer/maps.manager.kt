package wayzer

import arc.Events
import arc.files.Fi
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.contextScript
import cf.wayzer.scriptAgent.emit
import cf.wayzer.scriptAgent.thisContextScript
import coreLibrary.lib.config
import coreLibrary.lib.with
import coreMindustry.lib.broadcast
import coreMindustry.lib.game
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mindustry.Vars
import mindustry.core.GameState
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.game.Rules
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.io.MapIO
import mindustry.io.SaveIO
import java.util.logging.Level

/**
 * @param isSave when true, only "info.map.file" is valid
 */
class MapChangeEvent(val info: MapInfo, val isSave: Boolean, val rules: Rules) : Event,
    Event.Cancellable {
    /** Should call other load*/
    override var cancelled: Boolean = false

    companion object : Event.Handler()
}

object MapManager {
    var current: MapInfo = Vars.state.map?.let { //default may be useful, just for in case
        MapInfo(it.rules().idInTag, it, it.rules().mode())
    } ?: MapInfo(0, Vars.maps.all().first(), Gamemode.survival)
        private set

    @JvmOverloads
    fun loadMap(info: MapInfo = MapRegistry.nextMapInfo(), isSave: Boolean = false) {
        val event = MapChangeEvent(info, isSave, info.map.applyRules(info.mode).apply {
            idInTag = info.id
            Regex("\\[(@[a-zA-Z0-9]+)(=[^=\\]]+)?]").findAll(info.map.description()).forEach {
                val value = it.groupValues[2].takeIf(String::isNotEmpty) ?: "true"
                tags.put(it.groupValues[1], value.removePrefix("="))
            }
        }).emit()
        if (event.cancelled) return
        thisContextScript().launch(Dispatchers.game) {
            if (!Vars.net.server()) Vars.netServer.openServer()
            try {
                current.beforeReset?.invoke()
            } catch (e: Throwable) {
                thisContextScript().logger.log(Level.WARNING, "Error when do reset for $current", e)
            }
            val players = Groups.player.toList()
            Call.worldDataBegin()
            Vars.logic.reset()


            current = info
            try {
                info.load() // EventType.ResetEvent
                // EventType.SaveLoadEvent
                // EventType.WorldLoadEvent
            } catch (e: Throwable) {
                broadcast(
                    "[red]地图{info.map.name}无效:{reason}".with(
                        "info" to info,
                        "reason" to (e.message ?: "")
                    )
                )
                players.forEach { it.add() }
                loadMap()
                throw CancellationException()
            }
            Vars.state.map = info.map
            Vars.state.rules = event.rules.copy()


            if (isSave) {
                Vars.state.set(GameState.State.playing)
                Events.fire(EventType.PlayEvent())
            } else {
                Vars.logic.play()
            }
            // EventType.PlayEvent

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
    }

    fun loadSave(file: Fi) {
        val map = MapIO.createMap(file, true)
        val info = MapInfo(map.rules().idInTag, map, map.rules().mode()) {
            SaveIO.load(file)
        }
        loadMap(info, true)
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
    private val configTempSaveSlot by contextScript<Maps>().config.key(111, "临时缓存的存档格位")

    /** Use for identity Save */
    private var Rules.idInTag: Int
        get() = tags.getInt("id", -1)
        set(value) {
            tags.put("id", value.toString())
        }
}