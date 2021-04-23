package wayzer

import arc.Core
import arc.Events
import arc.files.Fi
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.contextScript
import cf.wayzer.scriptAgent.emit
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
class MapChangeEvent(val info: MapInfo, val isSave: Boolean, val applyMode: (Gamemode) -> Rules) : Event {
    /** useless when [isSave]*/
    var rules = applyMode(info.mode)

    override val handler = Companion

    companion object : Event.Handler()
}

object MapManager {
    private val script = contextScript<Maps>()
    fun loadMap(info: MapInfo = MapRegistry.nextMapInfo()) {
        resetAndLoad {
            val event = MapChangeEvent(info, false) { newMode ->
                info.map.applyRules(newMode).apply {
                    Regex("\\[(@[a-zA-Z0-9]+)(=[0-9a-z]+)?]").findAll(info.map.description()).forEach {
                        val value = it.groupValues[2].takeIf(String::isNotEmpty) ?: "true"
                        tags.put(it.groupValues[1], value.removePrefix("="))
                    }
                }
            }.emit()
            try {
                info.map.tags.put("id", info.id.toString())
                Vars.world.loadMap(info.map)
                Vars.state.rules = event.rules
                Vars.logic.play()
            } catch (e: MapException) {
                broadcast("[red]地图{info.map.name}无效:{reason}".with("info" to info, "reason" to (e.message ?: "")))
                return@resetAndLoad loadMap()
            }
        }
    }

    fun loadSave(file: Fi) {
        resetAndLoad {
            val map = MapIO.createMap(file, true)
            MapChangeEvent(MapInfo(0, map, map.rules().mode()), true) { map.rules() }.emit()
            SaveIO.load(file)
            Vars.state.set(GameState.State.playing)
            Events.fire(EventType.PlayEvent())
        }
    }

    fun getSlot(id: Int): Fi? {
        val file = SaveIO.fileFor(id)
        if (!SaveIO.isSaveValid(file)) return null
        val voteFile = SaveIO.fileFor(script.configTempSaveSlot)
        if (voteFile.exists()) voteFile.delete()
        file.copyTo(voteFile)
        return voteFile
    }

    //private
    private fun resetAndLoad(callBack: () -> Unit) {
        Core.app.post {
            if (!Vars.net.server()) Vars.netServer.openServer()
            val players = Groups.player.toList()
            players.forEach { it.clearUnit() }
            callBack()
            Call.worldDataBegin()
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