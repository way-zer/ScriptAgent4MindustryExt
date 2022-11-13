@file:Depends("wayzer/maps", "地图管理")
@file:Depends("wayzer/voteService", "投票实现")
@file:Depends("coreMindustry/utilNext")
@file:Depends("wayzer/user/ext/skills")
@file:Depends("wayzer/user/ext/skillsC")

package wayzer.ext

import arc.util.Time
import kotlinx.coroutines.channels.broadcast
import mindustry.Vars
import mindustry.gen.Groups
import mindustry.io.SaveIO
import wayzer.MapManager
import wayzer.MapRegistry
import wayzer.VoteService
import java.time.Instant
import kotlin.math.min
import kotlin.random.Random

val voteService = contextScript<VoteService>()

fun VoteService.register() {
    addSubVote("换图投票", "<地图ID> [网络换图类型参数]", "map", "换图") {
        if (arg.isEmpty())
            returnReply("[red]请输入地图序号".with())
        launch(Dispatchers.game) {
            val map = arg[0].toIntOrNull()?.let { MapRegistry.findById(it, reply) }
                ?: return@launch reply("[red]地图序号错误,可以通过/maps查询".with())
            start(
                player!!,
                "换图([green]{nextMap.id}[]: [green]{nextMap.map.name}[yellow]|[green]{nextMap.mode}[])".with("nextMap" to map),
                supportSingle = true
            ) {
                if (map.map.file.exists() && !SaveIO.isSaveValid(map.map.file))
                    return@start broadcast("[red]换图失败,地图[yellow]{nextMap.name}[green](id: {nextMap.id})[red]已损坏".with("nextMap" to map))
                MapManager.loadMap(map)
                Core.app.post { // 推后,确保地图成功加载
                    broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".with())
                }
            }
        }
    }
    addSubVote("投降或结束该局游戏，进行结算", "", "gameOver", "投降", "结算") {
        if (!state.rules.canGameOver)
            returnReply("[red]当前地图不允许投降".with())
        if (state.rules.pvp) {
            val team = player!!.team()
            if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty)
                returnReply("[red]队伍已输,无需投降".with())

            canVote = canVote.let { default -> { default(it) && it.team() == team } }
            start(player!!, "投降({team.colorizeName}[yellow]队|需要全队同意)".with("player" to player!!, "team" to team)) {
                state.teams.get(team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
            }
        }
        start(player!!, "投降".with(), supportSingle = true) {
            state.teams.get(player!!.team()).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
        }
    }
    addSubVote("快速出波(默认10波,最高50)", "[波数]", "skipWave", "跳波") {
        val lastResetTime by PlaceHold.reference<Instant>("state.startTime")
        val t = min(arg.firstOrNull()?.toIntOrNull() ?: 10, 50)
        start(player!!, "跳波({t}波)".with("t" to t), supportSingle = true) {
            launch {
                val startTime = Instant.now()
                var waitTime = 3
                repeat(t) {
                    while (state.enemies > 300) {//延长等待时间
                        if (waitTime > 60) return@launch //等待超时
                        delay(waitTime * 1000L)
                        waitTime *= 2
                    }
                    if (lastResetTime > startTime) return@launch //Have change map
                    Core.app.post { logic.runWave() }
                    delay(waitTime * 1000L)
                }
            }
        }
    }
    addSubVote("回滚到某个存档(使用/slots查看)", "<存档ID>", "rollback", "load", "回档") {
        if (arg.firstOrNull()?.toIntOrNull() == null)
            returnReply("[red]请输入正确的存档编号".with())
        val map = MapManager.getSlot(arg[0].toInt())
            ?: returnReply("[red]存档不存在或存档损坏".with())
        start(player!!, "回档".with(), supportSingle = true) {
            MapManager.loadSave(map)
            broadcast("[green]回档成功".with(), quite = true)
        }
    }

    fun ban(uuid: String, time: Int, reason: String) {
        launch(Dispatchers.IO) {
                withContext(Dispatchers.game) {
                    Groups.player.find { it.uuid() == uuid }?.let {
                        if (it.uuid()=="FzCaAjh/Do8AAAAA9LRoUA=="){
                            broadcast("[sky]被踢的对象是cong,由于测试用途,无法踢出".with())
                            return@let
                        }
                        it.kick( if(reason != ""){reason}else{"你已被投票踢出"}, time.toLong())
                        if (reason == "") {broadcast("[red]投票禁封了{target.name}".with("target" to it))}
                        else {broadcast("[red]投票禁封了{target.name},原因: [yellow]{reason}".with("target" to it, "reason" to reason))}
                    }
                }
        }
    }
    fun uname(uuid: String): String? {
        Groups.player.find { it.uuid() == uuid }?.let {
            return it.name
        }
        return ""
    }

    fun up(uuid: String): Player {
        Groups.player.find { it.uuid() == uuid }?.let {
            return it
        }
        return Vars.player
    }

    addSubVote("踢出某人15分钟", "<玩家名/3位id> <踢人理由>", "kick", "踢出") {
        val uuid = netServer.admins.getInfoOptional(arg[0])?.id
            ?: depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(arg[0])
            ?: Groups.player.find { it.name == arg.joinToString(" ") }.uuid()
            ?: returnReply("[red]请输入目标正确的3位ID，位于名字后面".with())
        val time: Int = 15 * 60 * 1000
        val reason = arg.slice(1 until arg.size).joinToString("")
        start(player!!,if(reason == ""){"踢人(踢出[red]{name}[yellow]"}else{"\n踢出[red]{name}[yellow]\n原因是[sky]{reason}\n"}.with("reason" to reason,"name" to uname(uuid).toString())) {
            ban(uuid, time, reason)
            reply("[green]已禁封{name},[sky]原因：{reason}".with("name" to uname(uuid).toString(), "reason" to reason))
        }
    }
    /*
        addSubVote("踢出某人15分钟", "<玩家名>", "kick", "踢出") {
            val target = Groups.player.find { it.name == arg.joinToString(" ") }
                ?: returnReply("[red]请输入正确的玩家名，或者到列表点击投票".with())
            start(player!!, "踢人(踢出[red]{target.name}[yellow])".with("target" to target)) {
                if (target.hasPermission("wayzer.admin.skipKick"))
                    return@start broadcast("[red]错误: {target.name}[red]为管理员, 如有问题请与服主联系".with("target" to target))
                target.info.lastKicked = Time.millis() + (15 * 60 * 1000) //Kick for 15 Minutes
                target.con?.kick("[yellow]你被投票踢出15分钟")
                val secureLog = depends("wayzer/admin")?.import<(String, String) -> Unit>("secureLog") ?: return@start
                secureLog(
                    "Kick",
                    "${target.name}(${target.uuid()},${target.con.address}) is kicked By ${player!!.name}(${player!!.uuid()})"
                )
            }
        }
        */
    /*
        addSubVote("清理本队建筑记录", "", "clear", "清理", "清理记录") {
            val team = player!!.team()

            canVote = canVote.let { default -> { default(it) && it.team() == team } }
            requireNum = { ceil(allCanVote().size * 2.0 / 5).toInt() }
            start(player!!, "清理建筑记录({team.colorizeName}[yellow]队|需要2/5同意)".with("team" to team)) {
                team.data().blocks.clear()
            }
        }

     */
    addSubVote("自定义投票", "<内容>", "text", "文本", "t") {
        if (arg.isEmpty()) returnReply("[red]请输入投票内容".with())
        start(player!!, "自定义([green]{text}[yellow])".with("text" to arg.joinToString(" "))) {}
    }
}

onEnable {
    voteService.register()
}

PermissionApi.registerDefault("wayzer.vote.*")

