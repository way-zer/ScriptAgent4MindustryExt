@file:Depends("wayzer/maps")

package wayzer.ext

import arc.Net
import arc.files.Fi
import arc.struct.StringMap
import arc.util.serialization.JsonReader
import arc.util.serialization.JsonValue
import arc.util.serialization.JsonWriter
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.game.Gamemode
import mindustry.gen.Groups
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
                val bytes by lazy { URL(downloadUrl).readBytes() }
                override fun read() = ByteArrayInputStream(bytes)
            }, tags.getInt("width", 0), tags.getInt("height"), tags, true)
            map.resourceId = hash
            MapInfo(id, map, mode ?: map.rules().mode())
        }
    }
})

fun <K, V> Map<K, V>.toJson(body: (V) -> String) = entries.joinToString(",", "{", "}") {
    "\"${it.key}\":${body(it.value)}"
}

//数据上报
fun postRecord(mapId: String, type: String, data: String) {
    if (!tokenOk) return
    Core.net.http(
        Net.HttpRequest(Net.HttpMethod.POST).url("$webRoot/api/maps/$mapId/record?token=$token&event=$type")
            .header("content-type", "application/json").content(data), {}, {})
}

listen<EventType.PlayEvent> {
    val map = state.map
    val id = map.resourceId ?: return@listen
    postRecord(id, "Start", mapOf("wave" to state.wave).toJson { it.toString() })
}
onEnable {
    launch {
        while (true) {
            delay(300_000)
            val id = state.map?.resourceId ?: continue
            postRecord(id, "Gaming", mapOf("wave" to state.wave).toJson { it.toString() })
        }
    }
}

//评分模块
val rateLimit = 30 * 60 //约30分钟的贡献
val stats = mutableMapOf<Long, Int>()

@Savable(false)
val rate = mutableMapOf<Long, Int>()
customLoad(::rate) {
    rate.putAll(it)
}
fun tryUpdateStats(p: Player) {
    val profile = PlayerData[p.uuid()].secureProfile(p)?.qq ?: return
    val score = "{p.statistics.score}".with("p" to p).toString().toFloatOrNull()?.toInt() ?: return
    val old = stats[profile] ?: 0
    stats[profile] = score
    arrayOf(30, 35, 40, 45, 50, 55, 60).find { it * 60 in (old + 1)..score }?.let { time ->
        if (profile in rate) return
        launch(Dispatchers.game) {
            p.sendMessage("[yellow]你已经游玩该图达到${time}分钟，有何感想? 输入/rate [red]1-10的整数[]评个分吧")
        }
    }
}

onEnable {
    launch {
        while (true) {
            delay(30_000)
            if (tokenOk && state.map.resourceId != null) {
                Groups.player.forEach(::tryUpdateStats)
            }
        }
    }
}

listen<EventType.GameOverEvent> {
    Groups.player.forEach(::tryUpdateStats)
}

listen<EventType.PlayEvent> {
    val id = state.map.resourceId ?: return@listen
    val data = mapOf("wave" to state.wave, "stats" to stats.filterValues { it > 15_000 }, "rate" to rate)
        .toJson { map ->
            if (map is Map<*, *>) map.toJson { it.toString() }
            else map.toString()
        }
    if (rate.isNotEmpty()) {
        val copy = rate.values.toList()
        launch(Dispatchers.game) {
            broadcast("[green]共用{num}人评分,平均分为{rate}".with("num" to copy.size, "rate" to copy.average()))
        }
    }
    stats.clear()
    rate.clear()
    postRecord(id, "End", data)
}

command("rate", "对地图进行评分") {
    type = CommandType.Client
    usage = "<1-10的整数>"
    aliases = listOf("评分")
    permission = "wayzer.rate"
    body {
        if (!tokenOk || state.map.resourceId == null)
            returnReply("[red]评分未开放(需要通过/vote web换图才能评分哦)".with())
        val score = arg.getOrNull(0)?.toIntOrNull() ?: replyUsage()
        if (score !in 1..10) replyUsage()
        val profile = PlayerData[player!!.uuid()].secureProfile(player!!)?.qq
            ?: returnReply("[red]仅绑定用户可以评分".with())
        if (stats[profile] ?: 0 < rateLimit)
            returnReply("[red]游玩该地图30分钟后再来评分".with())
        rate[profile] = score
        reply("[green]评分成功(修改请重新评分)".with())
    }
}