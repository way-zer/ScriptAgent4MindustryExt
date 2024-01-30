package wayzer

import arc.util.Log
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.emitAsync
import coreLibrary.lib.PlaceHoldString
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import mindustry.Vars
import mindustry.game.Gamemode
import mindustry.io.SaveIO
import mindustry.maps.Map

/** base mapInfo, not for load */
open class BaseMapInfo(val id: Int, val map: Map, val mode: Gamemode) {
    override fun toString(): String {
        return "BaseMapInfo(id=$id, map=$map, mode=$mode)"
    }

    override fun equals(other: Any?): Boolean = other is BaseMapInfo && id == other.id
    override fun hashCode(): Int = id
}

class MapInfo(
    id: Int, map: Map, mode: Gamemode,
    val beforeReset: (() -> Unit)? = null,
    /**use for generator or save*/
    val load: (() -> Unit) = {
        @Suppress("INACCESSIBLE_TYPE")
        SaveIO.load(map.file, Vars.world.filterContext(map))
    }
) : BaseMapInfo(id, map, mode) {
    override fun equals(other: Any?): Boolean = other is MapInfo && id == other.id
    override fun hashCode(): Int = id
}

abstract class MapProvider {
    abstract suspend fun searchMaps(search: String? = null): Collection<BaseMapInfo>
    /**@param id may not exist in getMaps*/
    open suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)? = null): MapInfo? =
        searchMaps().filterIsInstance<MapInfo>().find { it.id == id }
}

class GetNextMapEvent(val previous: BaseMapInfo?, var mapInfo: BaseMapInfo) : Event, Event.Cancellable {
    override var cancelled: Boolean = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

object MapRegistry : MapProvider() {
    private val providers = mutableSetOf<MapProvider>()
    fun register(script: Script, provider: MapProvider) {
        script.onDisable {
            providers.remove(provider)
        }
        providers.add(provider)
    }

    override suspend fun searchMaps(search: String?): List<BaseMapInfo> {
        @Suppress("NAME_SHADOWING")
        val search = search.takeUnless { it == "all" || it == "display" }
        return providers.flatMap { it.searchMaps(search) }
    }

    /**Dispatch should be Dispatchers.game*/
    override suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)?): MapInfo? {
        return providers.asFlow().map { it.findById(id, reply) }.filterNotNull().firstOrNull()
    }

    suspend fun nextMapInfo(
        previous: BaseMapInfo? = null,
        mode: Gamemode = Gamemode.survival,
        filter: String = "survive"
    ): MapInfo {
        val maps = searchMaps(filter).let { maps ->
            if (maps.isNotEmpty()) return@let maps
            Log.warn("服务器未安装地图,自动使用内置地图")
            Vars.maps.defaultMaps()
                .mapIndexed { i, map -> MapInfo(i + 1, map, Gamemode.survival) }
        }
        val next = maps.filter { it.mode == mode && it != previous }.randomOrNull() ?: maps.random()
        val info = GetNextMapEvent(previous, next).emitAsync().mapInfo
        if (info is MapInfo) return info
        return findById(info.id) ?: let {
            Log.err("Get search result ${info}, but can't find MapInfo implementation.")
            nextMapInfo(previous, mode, filter)
        }
    }
}