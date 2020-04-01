package wayzer

import arc.files.Fi
import mindustry.Vars
import mindustry.game.Gamemode
import mindustry.gen.Call
import mindustry.io.SaveIO
import mindustry.maps.Map

object ScriptConfig{
    const val configEnableInternMaps = false
    const val configTempSaveSlot = 111
}

object MapManager:SharedData.IMapManager{
    override val maps: Array<Map> get(){
        Vars.maps.reload()
        @Suppress("ConstantConditionIf")
        return if (ScriptConfig.configEnableInternMaps) Vars.maps.all().items else Vars.maps.customMaps()!!.items
    }

    override fun loadMap(map: Map, mode: Gamemode) {
        resetAndLoad {
            world.loadMap(map, map.applyRules(mode))
            state.rules = world.map.applyRules(mode)
            logic.play()
        }
    }

    override fun loadSave(file: Fi) {
        resetAndLoad {
            SaveIO.load(file)
            logic.play()
        }
    }

    override fun nextMap(map: Map?, mode: Gamemode): Map {
        val maps = maps.toMutableList()
        maps.shuffle()
        val ret = maps.filter { bestMode(it) == mode }.firstOrNull { it != map } ?: maps[0]
        if (!SaveIO.isSaveValid(ret.file)) {
            ContentHelper.logToConsole("[yellow]invalid map ${ret.file.nameWithoutExtension()}, auto change")
            return nextMap(map, mode)
        }
        return ret
    }

    override fun bestMode(map: Map): Gamemode {
        return when (map.file.name()[0]) {
            'A' -> Gamemode.attack
            'P' -> Gamemode.pvp
            'S' -> Gamemode.survival
            'C' -> Gamemode.sandbox
            'E' -> Gamemode.editor
            else -> Gamemode.bestFit(map.rules())
        }
    }

    private fun resetAndLoad(callBack: () -> Unit) {
        Core.app.post {
            val players = playerGroup.all().toList()
            players.forEach { it.dead = true }
            callBack()
            Call.onWorldDataBegin()
            players.forEach {
                if (it.con == null) return@forEach
                it.reset()
                if (state.rules.pvp)
                    it.team = netServer.assignTeam(it, players)
                netServer.sendWorldData(it)
            }
        }
    }

    override fun getSlot(id: Int): Fi? {
        val file = SaveIO.fileFor(id)
        if (!SaveIO.isSaveValid(file))return null
        val voteFile = SaveIO.fileFor(ScriptConfig.configTempSaveSlot)
        if (voteFile.exists()) voteFile.delete()
        file.copyTo(voteFile)
        return voteFile
    }
}
SharedData.mapManager = MapManager

//List maps
//List slots
//Game over
//Commands(host,load)
//autoHost