package wayzer

import arc.Core
import arc.Events
import arc.files.Fi
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.contextScript
import cf.wayzer.scriptAgent.emit
import coreLibrary.lib.config
import coreLibrary.lib.with
import coreMindustry.lib.broadcast
import mindustry.Vars
import mindustry.core.GameState
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.game.Rules
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.io.MapIO
import mindustry.io.SaveIO
import mindustry.maps.MapException

/**
 * @param isSave when true, only "info.map.file" is valid
 */
class MapChangeEvent(val info: MapInfo, val isSave: Boolean, val applyMode: (Gamemode) -> Rules) : Event,
    Event.Cancellable {
    /** useless when [isSave]*/
    var rules = applyMode(info.mode)

    /** Should call other load*/
    override var cancelled: Boolean = false

    override val handler = Companion

    companion object : Event.Handler()
}

object MapManager {
    var current: MapInfo = Vars.state.map?.let { //default may be useful, just for in case
        MapInfo(it.rules().idInTag, it, it.rules().mode())
    } ?: MapInfo(0, Vars.maps.all().first(), Gamemode.survival)
        private set

    fun loadMap(info: MapInfo = MapRegistry.nextMapInfo()) {
        val event = MapChangeEvent(info, false) { newMode ->
            info.map.applyRules(newMode).apply {
                Regex("\\[(@[a-zA-Z0-9]+)(=[^=\\]]+)?]").findAll(info.map.description()).forEach {
                    val value = it.groupValues[2].takeIf(String::isNotEmpty) ?: "true"
                    tags.put(it.groupValues[1], value.removePrefix("="))
                }
            }
        }.emit()
        if (event.cancelled) return
        resetAndLoad {
            current = info
            try {
                Vars.world.loadMap(info.map)
                Vars.state.rules = event.rules.apply {
                    idInTag = info.id
                }
                Vars.logic.play()
            } catch (e: MapException) {
                broadcast("[red]地图{info.map.name}无效:{reason}".with("info" to info, "reason" to (e.message ?: "")))
                return@resetAndLoad loadMap()
            }
        }
    }

    fun loadSave(file: Fi) {
        val map = MapIO.createMap(file, true)
        val info = MapInfo(map.rules().idInTag, map, map.rules().mode())
        if (MapChangeEvent(info, true) { map.rules() }.emit().cancelled) return
        resetAndLoad {
            current = info
            SaveIO.load(file)
            Vars.state.set(GameState.State.playing)
            Events.fire(EventType.PlayEvent())
        }
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
        get() = tags.getInt("id", 0)
        set(value) {
            tags.put("id", value.toString())
        }

    private fun resetAndLoad(callBack: () -> Unit) {
        Core.app.post {
            if (!Vars.net.server()) Vars.netServer.openServer()
            val players = Groups.player.toList()
            players.forEach { it.clearUnit() }
            Vars.logic.reset()
            Call.worldDataBegin()
            callBack()
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
}