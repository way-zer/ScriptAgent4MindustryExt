package wayzer

import arc.struct.StringMap
import arc.util.Log
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.emitAsync
import coreLibrary.lib.PlaceHoldString
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import mindustry.Vars
import mindustry.game.Gamemode
import mindustry.io.SaveIO
import mindustry.maps.MapException
import mindustry.maps.Map as MdtMap

data class MapInfo(
    val provider: MapProvider,
    val id: Int,
    val mode: Gamemode,
    var map: MdtMap? = null,
    val meta: Map<String, String> = emptyMap()
) {
    val name get() = meta["name"] ?: map?.name() ?: "unknown"
    val author get() = meta["author"] ?: map?.author() ?: "unknown"
    val description get() = meta["description"] ?: map?.description() ?: "[NULL]"

    suspend fun loadMap(): MdtMap {
        this.map?.let { return it }
        this.map = provider.lazyGetMap(this)
        return this.map!!
    }

    override fun equals(other: Any?): Boolean = other is MapInfo && (provider == other.provider && id == other.id)
    override fun hashCode(): Int = 31 * provider.hashCode() + id
    override fun toString(): String {
        return "MapInfo(name='$name', author='$author', mode=$mode, id=$id, provider=$provider)"
    }
}

abstract class MapProvider {
    abstract suspend fun searchMaps(search: String? = null): Collection<MapInfo>
    /**@param id may not exist in getMaps*/
    open suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)? = null): MapInfo? =
        searchMaps().find { it.id == id }

    open suspend fun lazyGetMap(info: MapInfo): MdtMap =
        throw NotImplementedError("you must implement `lazyGetMap` and provider when init MapInfo")

    open suspend fun loadMap(info: MapInfo) {
        //note: don't call this, as it catch Throwable inside, and not give result.
//        Vars.world.loadMap(info.loadMap())
        val map = info.loadMap()
        @Suppress("INACCESSIBLE_TYPE")
        SaveIO.load(map.file, Vars.world.filterContext(map))
        if (Vars.state.teams.getActive().none { it.hasCore() })
            throw MapException(map, "Map has no cores!")
    }
}

class GetNextMapEvent(val previous: MapInfo?, var mapInfo: MapInfo) : Event, Event.Cancellable {
    override var cancelled: Boolean = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

object MapRegistry : MapProvider() {
    /** Dumb object for GeneratorMap */
    val GeneratorMap by lazy { MdtMap(StringMap()) }
    private val providers = mutableSetOf<MapProvider>()
    fun register(script: Script, provider: MapProvider) {
        script.onDisable {
            providers.remove(provider)
        }
        providers.add(provider)
    }

    override suspend fun searchMaps(search: String?): List<MapInfo> {
        @Suppress("NAME_SHADOWING")
        val search = search.takeUnless { it == "all" || it == "display" }
        return coroutineScope {
            providers.map { async { it.searchMaps(search) } }
                .flatMap { it.await() }
        }
    }

    /**Dispatch should be Dispatchers.game*/
    override suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)?): MapInfo? {
        return providers.firstNotNullOfOrNull { it.findById(id, reply) }
    }

    suspend fun nextMapInfo(
        previous: MapInfo? = null,
        mode: Gamemode = Gamemode.survival
    ): MapInfo {
        val maps = searchMaps()
            .takeUnless { it.isEmpty() } ?: kotlin.run {
            Log.warn("服务器未安装地图,自动使用内置地图")
            searchMaps("@internal")
        }
        val next = maps.filter { it.mode == mode && it != previous }.randomOrNull() ?: maps.random()
        return GetNextMapEvent(previous, next).emitAsync().mapInfo
    }

    //not need register
    object SaveProvider : MapProvider() {
        override suspend fun searchMaps(search: String?): Collection<MapInfo> = emptyList()
        override suspend fun loadMap(info: MapInfo) {
            SaveIO.load(info.map!!.file)
        }
    }
}