package wayzer.ext

import arc.files.Fi
import arc.util.Time
import cf.wayzer.placehold.PlaceHoldContext
import kotlinx.coroutines.launch
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.io.MapIO
import mindustry.io.SaveIO
import java.io.InputStream
import java.net.URL
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

name = "投票"

val voteTime by config.key(Duration.ofSeconds(60)!!, "投票时间")
val enableWebMap by config.key(false,"是否允许网络地图","来自mdt.wayzer.top")

command("voteKick", "(弃用)投票踢人", "<player...>", CommandType.Client) { arg, p -> onVote(p!!, "kick", arg[0]) }
command("vote", "投票指令", "<map/gameOver/kick/skipWave/rollback> [params...]", CommandType.Client) { arg, p -> onVote(p!!, arg[0], arg.getOrNull(1)) }

val allSub = mutableMapOf<String, (Player, String?) -> Unit>()

class NetFi(private val url: URL,file:String):Fi(file){
    override fun read(): InputStream {
        return url.openStream()
    }
}

allSub["map"] = fun(p: Player, arg: String?) {
    if (arg == null)
        return p.sendMessage("[red]请输入地图序号".with())
    val maps = SharedData.mapManager.maps
    val map = when{
        Regex("[0-9a-z]{32}.*").matches(arg)->{
            if(!enableWebMap)return p.sendMessage("[red]本服未开启网络地图的支持".with())
            val (mapId,mode) = arg.split(' ').let {
                it[0] to it.getOrElse(1){"Q"}
            }
            MapIO.createMap(NetFi(URL("https://mdt.wayzer.top/api/maps/$mapId/download.msav"),mode+"download.msav"),true)
        }
        arg.toIntOrNull() in 1..maps.size->{
            maps[arg.toInt()-1]
        }
        else -> return p.sendMessage("[red]错误参数".with())
    }
    VoteHandler.apply {
        supportSingle = true
        start("换图({nextMap.id}: [yellow]{nextMap.name}[yellow])".with("nextMap" to map)) {
            if (!SaveIO.isSaveValid(map.file))
                return@start broadcast("[red]换图失败,地图[yellow]{nextMap.name}[green](id: {nextMap.id})[red]已损坏".with("nextMap" to map))
            SharedData.mapManager.loadMap(map)
            Core.app.post { // 推后,确保地图成功加载
                broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".with())
            }
        }
    }
}

allSub["gameOver".toLowerCase()] = fun(p: Player, _: String?) {
    if (state.rules.pvp) {
        val team = p.team
        if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty)
            return p.sendMessage("[red]队伍已输,无需投降".with())
        VoteHandler.apply {
            requireNum = { playerGroup.count { it.team == p.team } }
            canVote = { it.team == team }
            start("投降({player.name}[yellow]|{team.colorizeName}[yellow]队)".with("player" to p, "team" to team)) {
                state.teams.get(p.team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
            }
        }
        return
    }
    VoteHandler.apply {
        supportSingle = true
        start("投降".with()) {
            world.tiles.forEach { arr ->
                arr.filter { it.entity != null }.forEach {
                    Time.run(Random.nextFloat() * 60 * 6, it.entity::kill)
                }
            }
        }
    }
}

val lastResetTime by PlaceHold.reference<Date>("state.startTime")
allSub["skipWave".toLowerCase()] = fun(_: Player, arg: String?) {
    VoteHandler.apply {
        supportSingle = true
        val t = min(arg?.toIntOrNull() ?: 10,50)
        start("跳波({t}波)".with("t" to t)) {
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
}

allSub["rollback"] = fun(player: Player, arg: String?) {
    if (arg?.toIntOrNull() == null)
        return player.sendMessage("[red]请输入正确的存档编号".with())
    val map = SharedData.mapManager.getSlot(arg.toInt())
            ?: return player.sendMessage("[red]存档不存在或存档损坏".with())
    VoteHandler.apply {
        supportSingle = true
        start("回档".with()) {
            SharedData.mapManager.loadSave(map)
            broadcast("[green]回档成功".with(), quite = true)
        }
    }
}

allSub["kick"] = fun(player: Player, arg: String?) {
    val target = playerGroup.find { it.name == arg }
            ?: return player.sendMessage("[red]请输入正确的玩家名，或者到列表点击投票".with())
    if (SharedData.admin.isAdmin(player))
        return SharedData.admin.ban(player, target.uuid)
    VoteHandler.apply {
        start("踢人({player.name}[yellow]踢出[red]{target.name}[yellow])".with("player" to player, "target" to target)) {
            if (SharedData.admin.isAdmin(target)) {
                return@start broadcast("[red]错误: {target.name}[red]为管理员, 如有问题请与服主联系".with("target" to target))
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
    if (VoteHandler.voting.get()) return player.sendMessage("[red]投票进行中".with())
    if (type.toLowerCase() !in allSub) return player.sendMessage("[red]请检查输入是否正确".with())
    allSub[type.toLowerCase()]!!.invoke(player, arg)
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
        GlobalScope.launch {
            try {
                if (supportSingle) broadcast("[yellow]当前服务器只有一人,若投票结束前没人加入,则一人也可通过投票".with())
                broadcast("[yellow]{type}[yellow]投票开始,共需要{require}人,输入y或1同意".with("require" to requireNum()+1,"type" to voteDesc))
                repeat(voteTime.seconds.toInt()) {
                    delay(1000L)
                    if (voted.size > requireNum()) {//提前结束
                        broadcast("[yellow]{type}[yellow]投票结束,投票成功.[green]{voted}/{state.playerSize}[yellow],超过[red]{require}[yellow]人"
                                .with("type" to voteDesc, "voted" to voted.size, "require" to requireNum()))
                        Core.app.post(onSuccess)
                        return@launch
                    }
                }
                //TimeOut
                if (supportSingle && voted.size == 1) {
                    broadcast("[yellow]{type}[yellow]单人投票通过.".with("type" to voteDesc))
                    Core.app.post(onSuccess)
                } else {
                    broadcast("[yellow]{type}[yellow]投票结束,投票失败.[green]{voted}/{state.playerSize}[yellow],未超过[red]{require}[yellow]人"
                            .with("type" to voteDesc, "voted" to voted.size, "require" to requireNum()))
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
        if (p.uuid in voted) return p.sendMessage("[red]你已经投票".with())
        if (!canVote(p)) return p.sendMessage("[red]你不能对此投票".with())
        voted.add(p.uuid)
        broadcast("[green]投票成功,还需{left}人投票".with("left" to (requireNum() - voted.size + 1)), quite = true)
    }
}

listen<EventType.PlayerChatEvent> { e ->
    if (e.message.equals("y", true) || e.message == "1") VoteHandler.onVote(e.player)
}

listen<EventType.PlayerJoin> {
    if (!VoteHandler.voting.get()) return@listen
    VoteHandler.supportSingle = false
    it.player.sendMessage("[yellow]当前正在进行{type}[yellow]投票，输入y或1同意".with("type" to VoteHandler.voteDesc))
}