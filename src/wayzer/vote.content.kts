package wayzer

import arc.util.Time
import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.script_agent.util.ScheduleTask
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.io.SaveIO
import java.time.Duration
import kotlin.math.max
import kotlin.random.Random

object ScriptObject{
    val voteTime: Duration = Duration.ofSeconds(60)
}

//FIXME 结构过于复杂,无法运行
command("voteKick", "(弃用)投票踢人", "<player...>", CommandType.Client) { arg, p -> onVote(p!!, "kick", arg[0]) }
command("vote", "投票指令", "<map/gameOver/kick/skipWave/rollback> [params...]", CommandType.Client) { arg, p -> onVote(p!!, arg[0], arg.getOrNull(1)) }

val allSub = mutableMapOf<String, (Player, String?) -> (VoteHandler?)>()

allSub["map"] = fun(p: Player, arg: String?): VoteHandler? {
    if (arg == null) {
        p.sendMessage("[red]请输入地图序号".i18n())
        return null
    }
    val maps = SharedData.mapManager.maps
    val id = arg.toIntOrNull()
    if (id == null || id < 1 || id > maps.size) {
        p.sendMessage("[red]错误参数".i18n())
        return null
    }
    val map = maps[id - 1]
    return VoteHandler("换图({map.id}: [yellow]{map.name}[yellow])".i18n("_map" to map), {
        if (!SaveIO.isSaveValid(map.file))
            return@VoteHandler broadcast("[red]换图失败,地图[yellow]{map.name}[green](id: {map.id})[red]已损坏".i18n("_map" to map))
        SharedData.mapManager.loadMap(map)
        broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".i18n())
    }, true)
}

allSub["gameOver".toLowerCase()] = fun(p: Player, _: String?): VoteHandler? {
    if (state.rules.pvp) {
        val team = p.team
        if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty) {
            p.sendMessage("[red]队伍已输,无需投降".i18n())
            return null
        }
        return VoteHandler("投降({player.name}[yellow]|{team.colorizeName}[yellow]队)".i18n("player" to player, "team" to team), {
            state.teams.get(player.team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
        }, false, { playerGroup.count { it.team == p.team } }, { it.team == team })
    }
    return VoteHandler("投降".i18n(), {
        world.tiles.forEach { arr ->
            arr.forEach {
                Time.run(Random.nextFloat() * 60 * 6, it.entity::kill)
            }
        }
    }, true)
}

registerScheduleTask("runWave", task = ScheduleTask<Array<Long>> { first ->
    //data format: leftWaves waitTime
    if (first)
        data = arrayOf(((params.getOrNull(0) as? Int) ?: 10).toLong(), 3)
    //Have change map
    //完成或等待超时
    if (data[0] < 0 || data[1] > 60) return@ScheduleTask null
    if (state.enemies < 300) {
        data[0]--
        Core.app.post { logic.runWave() }
        return@ScheduleTask Duration.ofSeconds(data[1]).delayToDate()
    }
    //延长等待时间
    val time = data[1]
    data[1] = data[1] * 2
    return@ScheduleTask Duration.ofSeconds(time).delayToDate()
})

allSub["skipWave".toLowerCase()] = fun(_: Player, arg: String?): VoteHandler? {
    return VoteHandler("跳波".i18n(), {
        getScheduleTask("runWave").start(arg?.toIntOrNull() ?: 10)
    }, true)
}

allSub["rollback"] = fun(player: Player, arg: String?): VoteHandler? {
    if (arg?.toIntOrNull() == null) {
        player.sendMessage("[red]请输入正确的存档编号".i18n())
        return null
    }
    val map = SharedData.mapManager.getSlot(arg.toInt())
    if (map == null) {
        player.sendMessage("[red]存档不存在或存档损坏".i18n())
        return null
    }
    return VoteHandler("回档".i18n(), {
        SharedData.mapManager.loadSave(map)
        broadcast("[green]回档成功".i18n(), quite = true)
    }, true)
}

allSub["kick"] = fun(player: Player, arg: String?): VoteHandler? {
    val target = playerGroup.find { it.name == arg } ?: let {
        player.sendMessage("[red]请输入正确的玩家名，或者到列表点击投票".i18n())
        return null
    }
    if (SharedData.admin.isAdmin(player)) {
        SharedData.admin.ban(player, target.uuid)
        return null
    }
    return VoteHandler("踢人({player.name}[yellow]踢出[red]{target.name}[yellow])".i18n("player" to player, "target" to target), {
        if (SharedData.admin.isAdmin(target)) {
            return@VoteHandler broadcast("[red]错误: {target.name}[red]为管理员, 如有问题请与服主联系".i18n("target" to target))
        }
        if (target.info.timesKicked < 3) {
            target.info.lastKicked = Time.millis() + (15 * 60 * 1000) //Kick for 15 Minutes
            target.con?.kick("[yellow]你被投票踢出15分钟")
        } else
            netServer.admins.banPlayer(target.uuid)
        SharedData.admin.secureLog("Kick", "${target.name}(${target.uuid},${target.con.address}) is kicked By ${player.name}(${player.uuid})")
    })
}

fun onVote(player: Player, type: String, arg: String?) {
    if (type.toLowerCase() !in allSub) return player.sendMessage("[red]请检查输入是否正确".i18n())
    if (playerGroup.size() == 1) player.sendMessage("[yellow]当前服务器只有一人,若投票结束前没人加入,则一人也可通过投票(kick除外)".i18n())
    val ret = allSub[type]!!.invoke(player, arg)
    if (VoteHandler.inst != null) return player.sendMessage("[red]投票进行中".i18n())
    if (ret != null) {
        Call.sendMessage("/vote $type ${arg ?: ""}", mindustry.core.NetClient.colorizeName(player.id, player.name), player)
        VoteHandler.inst = ret
        ret.start()
    }
}

class VoteHandler(val type: PlaceHoldContext, val success: () -> Unit, singleVote: Boolean = false,
                  val require: () -> Int = { max(playerGroup.size() / 2, 1) },
                  val canVote: (Player) -> Boolean = { true }) : ScheduleTask<Boolean>({ first ->
    if (first) {//if params not empty means run now
        data = singleVote && playerGroup.size() <= 1 //一人投票标记
        broadcast("[yellow]{type}[yellow]投票开始,输入y同意".i18n("type" to type))
        ScriptObject.voteTime.delayToDate()
    } else {
        (this as VoteHandler).endCheck()
        null
    }
}) {
    fun endCheck() {
        val requireN = require()
        if (voted.size > requireN) {
            broadcast("[yellow]{type}[yellow]投票结束,投票成功.[green]{voted}/{state.playerSize}[yellow],超过[red]{require}[yellow]人"
                    .i18n("type" to type, "voted" to voted.size, "require" to requireN))
            success()
        } else if (data && voted.size == 1) {
            broadcast("[yellow]{type}[yellow]单人投票通过.".i18n("type" to type))
            success()
        } else {
            broadcast("[yellow]{type}[yellow]投票结束,投票失败.[green]{voted}/{state.playerSize}[yellow],未超过[red]{require}[yellow]人"
                    .i18n("type" to type, "voted" to voted.size, "require" to requireN))
        }
        inst = null
    }

    companion object {
        var inst: VoteHandler? = null
        val voted = mutableListOf<String>()
        fun onVote(p: Player) {
            if (inst == null) return
            if (p.uuid in voted) return p.sendMessage("[red]你已经投票".i18n())
            if (inst?.canVote?.invoke(p) != true) return p.sendMessage("[red]你不能对此投票".i18n())
            voted.add(p.uuid)
            broadcast("[green]投票成功".i18n(), quite = true)
            if ((inst?.require?.invoke() ?: Int.MAX_VALUE) < voted.size) {
                inst?.cancel()
                inst?.endCheck()
            }
        }
    }
}

listen<EventType.PlayerChatEvent> { e ->
    if (e.message.equals("y", true)) VoteHandler.onVote(e.player)
}

listen<EventType.PlayerJoin> {
    if (VoteHandler.inst == null) return@listen
    player.sendMessage("[yellow]当前正在进行{type}[yellow]投票，输入y同意".i18n("type" to VoteHandler.inst!!.type))
    VoteHandler.inst?.data = false //有他人加入,取消一人投票
}

listen<EventType.ResetEvent> {
    getScheduleTask("runWave").cancel()
    VoteHandler.inst?.cancel()
    VoteHandler.inst = null
}