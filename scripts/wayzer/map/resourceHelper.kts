@file:Depends("wayzer/maps")

package wayzer.map

import arc.files.Fi
import arc.util.Strings
import arc.util.serialization.Jval
import com.google.common.cache.CacheBuilder
import mindustry.game.Gamemode
import mindustry.io.MapIO
import wayzer.MapInfo
import wayzer.MapProvider
import wayzer.MapRegistry
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URL
import java.net.URLEncoder
import java.time.Duration
import java.util.logging.Level

name = "资源站配套脚本"

val webRoot by config.key("https://api.mindustry.top", "Mindustry资源站Api")

fun Jval.toStringMap(): Map<String, String> = buildMap {
    asObject().forEach {
        if (it.value.isString) {
            put(it.key, it.value.asString())
        }
    }
}

suspend fun httpGet(url: String, retry: Int = 3) = withContext(Dispatchers.IO) {
    var result: Result<ByteArray> = Result.failure(IllegalStateException("result not set"))
    repeat(retry + 1) {
        result = kotlin.runCatching {
            val stream = URL(url).openConnection()
                .apply { readTimeout = 3_000 }
                .getInputStream()
            stream.use { runInterruptible { stream.readBytes() } }
        }.onSuccess { return@withContext it }
        delay(1000)
    }
    result.getOrThrow()
}


MapRegistry.register(this, object : MapProvider() {
    val searchCache = CacheBuilder.newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build<String, List<MapInfo>>()!!

    override suspend fun searchMaps(search: String?): Collection<MapInfo> {
        val provider = this
        val mappedSearch = when (search) {
            "all", "display", "site", null -> ""
            "pvp", "attack", "survive" -> "@mode:${Strings.capitalize(search)}"
            else -> search
        }
        searchCache.getIfPresent(mappedSearch)?.let { return it }
        try {
            @Suppress("BlockingMethodInNonBlockingContext")
            val maps =
                httpGet("$webRoot/maps/list?prePage=100&search=${URLEncoder.encode(mappedSearch, "utf-8")}", retry = 1)
                    .let { Jval.read(it.toString(Charsets.UTF_8)).asArray() }
                    .asIterable().map { info ->
                        val id = info.getInt("id", -1)
                        val mode = info.getString("mode", "unknown")
                        MapInfo(
                            provider, id,
                            Gamemode.all.find { it.name.equals(mode, ignoreCase = true) } ?: Gamemode.survival,
                            meta = info.toStringMap()
                        )
                    }
            searchCache.put(mappedSearch, maps)
            return maps
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Fail to searchMap($search)", e)
            return emptyList()
        }
    }

    override suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)?): MapInfo? {
        if (id !in 10000..99999) return null
        try {
            val info = httpGet("$webRoot/maps/$id.json")
                .let { Jval.read(it.toString(Charsets.UTF_8)) }
            val mode = info.getString("mode", "unknown")
            return MapInfo(
                this, id,
                Gamemode.all.find { it.name.equals(mode, ignoreCase = true) } ?: Gamemode.survival,
                meta = info.get("tags").toStringMap(),
            )
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Fail to findById($id)", e)
            return null
        }
    }

    override suspend fun lazyGetMap(info: MapInfo): mindustry.maps.Map {
        val bs = httpGet("$webRoot/maps/${info.id}.msav", retry = 3)
        val fi = object : Fi("BYTES.msav") {
            override fun read(): InputStream {
                return ByteArrayInputStream(bs)
            }
        }
        return MapIO.createMap(fi, true)
    }
})
