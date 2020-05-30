package wayzer.ext

import arc.util.Time
import cf.wayzer.placehold.PlaceHoldContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.io.SaveIO
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.random.Random

name = "投票"

val voteTime by config.key(Duration.ofSeconds(60)!!, "投票时间")

command("voteKick", "(弃用)投票踢人", "<player...>", CommandType.Client) { arg, p -> onVote(p!!, "kick", arg[0]) }
command("vote", "投票指令", "<map/gameOver/kick/skipWave/rollback> [params...]", CommandType.Client) { arg, p -> onVote(p!!, arg[0], arg.getOrNull(1)) }

val allSub = mutableMapOf<String, (Player, String?) -> Unit>()

allSub["map"] = fun(p: Player, arg: String?) {
    if (arg == null)
        return p.sendMessage("[red]请输入地图序号".i18n())
    val maps = SharedData.mapManager.maps
    val id = arg.toIntOrNull()
    if (id == null || id < 1 || id > maps.size)
        return p.sendMessage("[red]错误参数".i18n())
    val map = maps[id - 1]
    VoteHandler.apply {
        supportSingle = true
        start("换图({nextMap.id}: [yellow]{nextMap.name}[yellow])".i18n("nextMap" to map)) {
            if (!SaveIO.isSaveValid(map.file))
                return@start broadcast("[red]换图失败,地图[yellow]{nextMap.name}[green](id: {nextMap.id})[red]已损坏".i18n("nextMap" to map))
            SharedData.mapManager.loadMap(map)
            broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".i18n())
        }
    }
}

allSub["gameOver".toLowerCase()] = fun(p: Player, _: String?) {
    if (state.rules.pvp) {
        val team = p.team
        if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty)
            return p.sendMessage("[red]队伍已输,无需投降".i18n())
        VoteHandler.apply {
            requireNum = { playerGroup.count { it.team == p.team } }
            canVote = { it.team == team }
            start("投降({player.name}[yellow]|{team.colorizeName}[yellow]队)".i18n("player" to player, "team" to team)) {
                state.teams.get(player.team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
            }
        }
        return
    }
    VoteHandler.apply {
        supportSingle = true
        start("投降".i18n()) {
            world.tiles.forEach { arr ->
                arr.filter { it.entity != null }.forEach {
                    Time.run(Random.nextFloat() * 60 * 6, it.entity::kill)
                }
            }
        }
    }
}

private val lastResetTime by PlaceHold.reference<Date>("state.startTime")
allSub["skipWave".toLowerCase()] = fun(_: Player, arg: String?) {
    VoteHandler.apply {
        supportSingle = true
        start("跳波".i18n()) {
            SharedCoroutineScope.launch {
                val startTime = Time.millis()
                var waitTime = 3
                repeat(arg?.toIntOrNull() ?: 10) {
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
}

allSub["rollback"] = fun(player: Player, arg: String?) {
    if (arg?.toIntOrNull() == null)
        return player.sendMessage("[red]请输入正确的存档编号".i18n())
    val map = SharedData.mapManager.getSlot(arg.toInt())
            ?: return player.sendMessage("[red]存档不存在或存档损坏".i18n())
    VoteHandler.apply {
        supportSingle = true
        start("回档".i18n()) {
            SharedData.mapManager.loadSave(map)
            broadcast("[green]回档成功".i18n(), quite = true)
        }
    }
}

allSub["kick"] = fun(player: Player, arg: String?) {
    val target = playerGroup.find { it.name == arg }
            ?: return player.sendMessage("[red]请输入正确的玩家名，或者到列表点击投票".i18n())
    if (SharedData.admin.isAdmin(player))
        return SharedData.admin.ban(player, target.uuid)
    VoteHandler.apply {
        start("踢人({player.name}[yellow]踢出[red]{target.name}[yellow])".i18n("player" to player, "target" to target)) {
            if (SharedData.admin.isAdmin(target)) {
                return@start broadcast("[red]错误: {target.name}[red]为管理员, 如有问题请与服主联系".i18n("target" to target))
            }
            if (target.info.timesKicked < 3) {
                target.info.lastKicked = Time.millis() + (15 * 60 * 1000) //Kick for 15 Minutes
                target.con?.kick("[yellow]你被投票踢出15分钟")
            } else
                netServer.admins.banPlayer(target.uuid)
            SharedData.admin.secureLog("Kick", "${target.name}(${target.uuid},${target.con.address}) is kicked By ${player.name}(${player.uuid})")
        }
    }
}

fun onVote(player: Player, type: String, arg: String?) {
    if (VoteHandler.voting.get()) return player.sendMessage("[red]投票进行中".i18n())
    if (type.toLowerCase() !in allSub) return player.sendMessage("[red]请检查输入是否正确".i18n())
    allSub[type]!!.invoke(player, arg)
    if (VoteHandler.voting.get()) {//success
        Call.sendMessage("/vote $type ${arg ?: ""}", mindustry.core.NetClient.colorizeName(player.id, player.name), player)
    }
}

//instead of object because object can't be inner
@Suppress("PropertyName")
val VoteHandler = VoteHandler()

inner class VoteHandler {
    //private set
    val voting = AtomicBoolean(false)
    private val voted: MutableSet<String> = ConcurrentHashMap.newKeySet()

    var supportSingle = false
    lateinit var voteDesc: PlaceHoldContext
    lateinit var requireNum: () -> Int
    lateinit var canVote: (Player) -> Boolean

    init {
        reset()
    }

    fun start(voteDesc: PlaceHoldContext, onSuccess: () -> Unit) {
        if (voting.get()) return
        voting.set(true)
        this.voteDesc = voteDesc
        supportSingle = supportSingle && playerGroup.size() <= 1
        SharedCoroutineScope.launch {
            try {
                if (supportSingle) broadcast("[yellow]当前服务器只有一人,若投票结束前没人加入,则一人也可通过投票".i18n())
                broadcast("[yellow]{type}[yellow]投票开始,输入y或1同意".i18n("type" to voteDesc))
                repeat(voteTime.seconds.toInt()) {
                    delay(1000L)
                    if (voted.size > requireNum()) {//提前结束
                        broadcast("[yellow]{type}[yellow]投票结束,投票成功.[green]{voted}/{state.playerSize}[yellow],超过[red]{require}[yellow]人"
                                .i18n("type" to voteDesc, "voted" to voted.size, "require" to requireNum()))
                        Core.app.post(onSuccess)
                        return@launch
                    }
                }
                //TimeOut
                if (supportSingle && voted.size == 1) {
                    broadcast("[yellow]{type}[yellow]单人投票通过.".i18n("type" to voteDesc))
                    Core.app.post(onSuccess)
                } else {
                    broadcast("[yellow]{type}[yellow]投票结束,投票失败.[green]{voted}/{state.playerSize}[yellow],未超过[red]{require}[yellow]人"
                            .i18n("type" to voteDesc, "voted" to voted.size, "require" to requireNum()))
                }
            } finally {
                reset()
            }
        }
    }

    private fun reset() {
        supportSingle = false
        requireNum = { max(playerGroup.size() / 2, 1) }
        canVote = { true }
        voted.clear()
        voting.set(false)
    }

    fun onVote(p: Player) {
        if (!voting.get()) return
        if (p.uuid in voted) return p.sendMessage("[red]你已经投票".i18n())
        if (!canVote(p)) return p.sendMessage("[red]你不能对此投票".i18n())
        voted.add(p.uuid)
        broadcast("[green]投票成功,还需{left}人投票".i18n("left" to (requireNum() - requireNum() + 1)), quite = true)
    }
}

listen<EventType.PlayerChatEvent> { e ->
    if (e.message.equals("y", true) || e.message == "1") VoteHandler.onVote(e.player)
}

listen<EventType.PlayerJoin> {
    if (!VoteHandler.voting.get()) return@listen
    VoteHandler.supportSingle = false
    player.sendMessage("[yellow]当前正在进行{type}[yellow]投票，输入y或1同意".i18n("type" to VoteHandler.voteDesc))
}