@file:Depends("wayzer/maps")

package wayzer.ext

import arc.files.Fi
import arc.struct.StringMap
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.game.Gamemode
import wayzer.MapInfo
import wayzer.MapProvider
import wayzer.MapRegistry
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import mindustry.maps.Map as MdtMap

name = "资源站配套脚本"

val token by config.key("", "Mindustry资源站服务器Token")
val webRoot by config.key("https://mdt.wayzer.top", "Mindustry资源站Api")

val tokenOk get() = token.isNotBlank()

var MdtMap.resourceId: String?
    get() = tags.get("resourceId")
    set(v) {
        tags.put("resourceId", v)
    }

fun JsonValue.toStringMap() = StringMap().apply {
    var node = child()
    do {
        put(node.name, node.toJson(JsonWriter.OutputType.minimal))
        node = node.next
    } while (node != null)
}

MapRegistry.register(this, object : MapProvider() {
    override fun getMaps(filter: String): Collection<MapInfo> {
        return emptyList()
    }

    override suspend fun findById(id: Int, reply: ((PlaceHoldString) -> Unit)?): MapInfo? {
        if (id !in 10000..99999) return null
        if (!tokenOk) {
            reply?.invoke("[red]本服未开启网络换图，请联系服主开启".with())
            return null
        }
        return withContext(Dispatchers.IO) {
            val info = let {
                val infoUrl = "$webRoot/api/maps/thread/$id/latest"
                val infoCon = URL(infoUrl).openConnection() as HttpURLConnection
                infoCon.connect()
                if (infoCon.responseCode != HttpURLConnection.HTTP_OK) {
                    launch(Dispatchers.game) {
                        reply?.invoke("[red]网络地图加载失败,请稍后再试:{msg}".with("msg" to infoCon.responseMessage))
                    }
                    return@withContext null
                }
                infoCon.inputStream.use { JsonReader().parse(it.readBytes().decodeToString()) }
            }
            val hash = info.getString("hash")
            val tags = info.get("tags").toStringMap()
            val mode = info.getString("mode", "unknown").let { mode ->
                if (mode.equals("unknown", true)) return@let null
                Gamemode.all.find { it.name.equals(mode, ignoreCase = true) } ?: Gamemode.survival
            }

            val map = mindustry.maps.Map(object : Fi("file.msav") {
                val downloadUrl = "$webRoot/api/maps/$hash/downloadServer?token=$token"
                val bytes by lazy {
                    URL(downloadUrl).openConnection().apply {
                        readTimeout = 10_000
                    }.getInputStream().readBytes()
                }

                override fun read() = ByteArrayInputStream(bytes)
                override fun exists() = true
            }, tags.getInt("width", 0), tags.getInt("height"), tags, true)
            map.resourceId = hash
            MapInfo(id, map, mode ?: map.rules().mode())
        }
    }
})
