@file:Import("@wayzer/services/VoteService.kt", sourceFile = true)
@file:Import("@wayzer/services/MapService.kt", sourceFile = true)

package wayzer.ext

import arc.Net
import arc.files.Fi
import arc.util.serialization.JsonReader
import io.ktor.http.*
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.io.MapIO
import wayzer.services.MapService
import wayzer.services.VoteService
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

name = "资源站配套脚本"

val token by config.key("", "Mindustry资源站服务器Token")
val webRoot by config.key("https://mdt.wayzer.top/", "Mindustry资源站Api")

val tokenOk get() = token.isNotBlank()

suspend fun get(path: String): Net.HttpResponse {
    return suspendCoroutine { c ->
        Core.net.httpGet(webRoot + path, {
            c.resume(it)
        }, { c.resumeWithException(it) })
    }
}

val voteService by ServiceRegistry<VoteService>()
val mapService by ServiceRegistry<MapService>()

fun VoteService.registerCommand() {
    addSubVote("网络换图", "<地图识别码>", "web") {
        if (!tokenOk) returnReply("[red]本服未开启网络换图，请联系服主开启".with())
        if (arg.isEmpty() || Regex("[0-9a-z]{32}.*").matches(arg[0]))
            returnReply("[red]请输入地图识别码,识别码可到[yellow]{web}[]上查询".with("web" to webRoot))
        reply("[green]加载网络地图中".with())
        launch(Dispatchers.IO) {
            val info = get("/api/maps/${arg[0]}/downloadServer?token=$token").let {
                if (it.status != Net.HttpStatus.OK) {
                    launch(Dispatchers.game) {
                        reply("[red]网络地图加载失败,请稍后再试:{msg}".with("msg" to it.resultAsString))
                    }
                    return@launch
                }
                JsonReader().parse(it.resultAsString)
            }
            val name = info.getString("name", "未知")
            val mode = info.getString("mode", "").let { mode ->
                Gamemode.all.find { it.name.equals(mode, ignoreCase = true) } ?: Gamemode.survival
            }
            start(
                player!!, "网络换图[yellow]({name}[yellow]|[green]{mode}[])"
                    .with("name" to name, "mode" to mode), true
            ) {
                depends("wayzer/user/ext/statistics")?.import<(Team) -> Unit>("onGameOver")?.invoke(Team.derelict)
                mapService.loadMap(MapIO.createMap(object : Fi("$name.msav") {
                    override fun read() = URL(info.getString("download")).openStream()
                }, true))
                Core.app.post { // 推后,确保地图成功加载
                    broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".with())
                }
            }
        }
    }
}


onEnable {
    voteService.registerCommand()
}