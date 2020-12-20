@file:Import("@wayzer/services/VoteService.kt", sourceFile = true)
@file:Import("@wayzer/services/MapService.kt", sourceFile = true)

package wayzer.ext

import arc.files.Fi
import arc.util.Time
import cf.wayzer.script_agent.util.ServiceRegistry
import mindustry.game.Team
import mindustry.gen.Player
import mindustry.io.MapIO
import mindustry.io.SaveIO
import wayzer.services.MapService
import wayzer.services.VoteService
import java.io.InputStream
import java.net.URL
import java.util.*
import kotlin.math.ceil
import kotlin.math.min
import kotlin.random.Random

val voteService by ServiceRegistry<VoteService>()
val mapService by ServiceRegistry<MapService>()

val enableWebMap by config.key(false, "是否允许网络地图", "来自mdt.wayzer.top")

class NetFi(private val url: URL, file: String) : Fi(file) {
    override fun read(): InputStream {
        return url.openStream()
    }
}

fun VoteService.register() {
    addSubVote("换图投票", "<地图ID> [网络换图类型参数]", "map", "换图") {
        if (arg.isEmpty())
            returnReply("[red]请输入地图序号".with())
        val maps = mapService.maps
        launch(Dispatchers.game) {
            val map = when {
                Regex("[0-9a-z]{32}.*").matches(arg[0]) -> {
                    if (!enableWebMap) return@launch reply("[red]本服未开启网络地图的支持".with())
                    val mode = arg.getOrElse(1) { "Q" }
                    reply("[green]加载网络地图中".with())
                    try {
                        withContext(Dispatchers.IO) {
                            @Suppress("BlockingMethodInNonBlockingContext")
                            MapIO.createMap(
                                NetFi(
                                    URL("https://mdt.wayzer.top/api/maps/${arg[0]}/download.msav"),
                                    mode + "download.msav"
                                ), true
                            )
                        }
                    } catch (e: Exception) {
                        reply("[red]网络地图加载失败,请稍后再试".with())
                        throw e
                    }
                }
                arg[0].toIntOrNull() in 1..maps.size -> {
                    maps[arg[0].toInt() - 1]
                }
                else -> return@launch reply("[red]错误参数".with())
            }
            start(
                player!!,
                "换图({nextMap.id}: [yellow]{nextMap.name}[yellow])".with("nextMap" to map),
                supportSingle = true
            ) {
                if (!SaveIO.isSaveValid(map.file))
                    return@start broadcast("[red]换图失败,地图[yellow]{nextMap.name}[green](id: {nextMap.id})[red]已损坏".with("nextMap" to map))
                depends("wayzer/user/statistics")?.import<(Team) -> Unit>("onGameOver")?.invoke(Team.derelict)
                mapService.loadMap(map)
                Core.app.post { // 推后,确保地图成功加载
                    broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".with())
                }
            }
        }
    }
    addSubVote("投降或结束该局游戏，进行结算", "", "gameOver", "投降", "结算") {
        if (state.rules.pvp) {
            val team = player!!.team()
            if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty)
                returnReply("[red]队伍已输,无需投降".with())

            canVote = canVote.let { default -> { default(it) && it.team() == team } }
            requireNum = { allCanVote().size }
            start(player!!, "投降({team.colorizeName}[yellow]队|需要全队同意)".with("player" to player!!, "team" to team)) {
                state.teams.get(team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
            }
        }
        start(player!!, "投降".with(), supportSingle = true) {
            state.teams.get(player!!.team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
        }
    }
    addSubVote("快速出波(默认10波,最高50)", "[波数]", "skipWave", "跳波") {
        val lastResetTime by PlaceHold.reference<Date>("state.startTime")
        val t = min(arg.firstOrNull()?.toIntOrNull() ?: 10, 50)
        start(player!!, "跳波({t}波)".with("t" to t), supportSingle = true) {
            launch {
                val startTime = Time.millis()
                var waitTime = 3
                repeat(t) {
                    while (state.enemies > 300) {//延长等待时间
                        if (waitTime > 60) return@launch //等待超时
                        delay(waitTime * 1000L)
                        waitTime *= 2
                    }
                    if (lastResetTime.time > startTime) return@launch //Have change map
                    Core.app.post { logic.runWave() }
                    delay(waitTime * 1000L)
                }
            }
        }
    }
    addSubVote("回滚到某个存档(使用/slots查看)", "<存档ID>", "rollback", "load", "回档") {
        if (arg.firstOrNull()?.toIntOrNull() == null)
            returnReply("[red]请输入正确的存档编号".with())
        val map = mapService.getSlot(arg[0].toInt())
            ?: returnReply("[red]存档不存在或存档损坏".with())
        start(player!!, "回档".with(), supportSingle = true) {
            depends("wayzer/user/statistics")?.import<(Team) -> Unit>("onGameOver")?.invoke(Team.derelict)
            mapService.loadSave(map)
            broadcast("[green]回档成功".with(), quite = true)
        }
    }
    addSubVote("踢出某人15分钟", "<玩家名>", "kick", "踢出") {
        val target = playerGroup.find { it.name == arg.joinToString(" ") }
            ?: returnReply("[red]请输入正确的玩家名，或者到列表点击投票".with())
        val adminBan = depends("wayzer/admin")?.import<(Player, String) -> Unit>("ban")
        if (hasPermission("wayzer.vote.ban") && adminBan != null) {
            return@addSubVote adminBan(player!!, target.uuid)
        }
        start(player!!, "踢人(踢出[red]{target.name}[yellow])".with("target" to target)) {
            target.info.lastKicked = Time.millis() + (15 * 60 * 1000) //Kick for 15 Minutes
            target.con?.kick("[yellow]你被投票踢出15分钟")
            val secureLog = depends("wayzer/admin")?.import<(String, String) -> Unit>("secureLog") ?: return@start
            secureLog(
                "Kick",
                "${target.name}(${target.uuid},${target.con.address}) is kicked By ${player!!.name}(${player!!.uuid})"
            )
        }
    }
    addSubVote("清理本队建筑记录", "", "clear", "清理", "清理记录") {
        val team = player!!.team

        canVote = canVote.let { default -> { default(it) && it.team == team } }
        requireNum = { ceil(allCanVote().size * 2.0 / 5).toInt() }
        start(player!!, "清理建筑记录({team.colorizeName}[yellow]队|需要2/5同意)".with("team" to team)) {
            team.data().blocks.clear()
        }
    }
}

onEnable {
    voteService.register()
}