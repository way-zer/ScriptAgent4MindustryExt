//@file:Depends("wayzer/user/statistics","玩家活跃判定",soft = true)
package wayzer.ext

import arc.files.Fi
import arc.util.Time
import cf.wayzer.placehold.PlaceHoldContext
import coreMindustry.lib.util.sendMenuPhone
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
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

name = "投票"

val voteTime by config.key(Duration.ofSeconds(60)!!, "投票时间")
val enableWebMap by config.key(false, "是否允许网络地图", "来自mdt.wayzer.top")

inner class VoteCommands : Commands() {
    override fun invoke(context: CommandContext) {
        if (VoteHandler.voting.get()) return context.reply("[red]投票进行中".with())
        super.invoke(context)
        if (VoteHandler.voting.get()) {//success
            Call.sendMessage(context.prefix + context.arg.joinToString(" "), mindustry.core.NetClient.colorizeName(context.player!!.id, context.player!!.name), context.player!!)
        }
    }

    override fun onHelp(context: CommandContext, explicit: Boolean) {
        if (!explicit) context.reply("[red]错误投票类型,请检查输入是否正确".with())
        context.sendMenuPhone("可用投票类型", subCommands.values.toSet().filter {
            it.permission.isBlank() || context.hasPermission(it.permission)
        }, 1, 100) {
            context.helpInfo(it, false)
        }
    }
}

val voteCommands = VoteCommands()
command("vote", "发起投票", {
    type = CommandType.Client
    aliases = listOf("投票")
}, voteCommands)
command("votekick", "(弃用)投票踢人", { this.usage = "<player...>";this.type = CommandType.Client }) {
    //Redirect
    arg = listOf("kick", *arg.toTypedArray())
    voteCommands.invoke(this)
}

export(::voteCommands)

fun subVote(desc: String, usage: String, vararg aliases: String, body: CommandContext.() -> Unit) {
    voteCommands += CommandInfo(null, aliases.first(), desc) {
        this.usage = usage
        this.aliases = aliases.toList()
        body(body)
    }
}

class NetFi(private val url: URL, file: String) : Fi(file) {
    override fun read(): InputStream {
        return url.openStream()
    }
}
subVote("换图投票", "<地图ID> [网络换图类型参数]", "map", "换图") {
    if (arg.isEmpty())
        return@subVote reply("[red]请输入地图序号".with())
    val maps = SharedData.mapManager.maps
    val map = when {
        Regex("[0-9a-z]{32}.*").matches(arg[0]) -> {
            if (!enableWebMap) return@subVote reply("[red]本服未开启网络地图的支持".with())
            val mode = arg.getOrElse(1) { "Q" }
            MapIO.createMap(NetFi(URL("https://mdt.wayzer.top/api/maps/${arg[0]}/download.msav"), mode + "download.msav"), true)
        }
        arg[0].toIntOrNull() in 1..maps.size -> {
            maps[arg[0].toInt() - 1]
        }
        else -> return@subVote reply("[red]错误参数".with())
    }
    VoteHandler.start(player!!, "换图({nextMap.id}: [yellow]{nextMap.name}[yellow])".with("nextMap" to map), supportSingle = true) {
        if (!SaveIO.isSaveValid(map.file))
            return@start broadcast("[red]换图失败,地图[yellow]{nextMap.name}[green](id: {nextMap.id})[red]已损坏".with("nextMap" to map))
        SharedData.mapManager.loadMap(map)
        Core.app.post { // 推后,确保地图成功加载
            broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".with())
        }
    }
}
subVote("投降或结束该局游戏，进行结算", "", "gameOver", "投降", "结算") {
    if (state.rules.pvp) {
        val team = player!!.team
        if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty)
            return@subVote reply("[red]队伍已输,无需投降".with())
        VoteHandler.apply {
            canVote = canVote.let { default -> { default(it) && it.team == team } }
            start(player!!, "投降({team.colorizeName}[yellow]队|需要全队同意)".with("player" to player!!, "team" to team)) {
                state.teams.get(team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
            }
        }
        return@subVote
    }
    VoteHandler.start(player!!, "投降".with(), supportSingle = true) {
        state.teams.get(player!!.team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
    }
}
val lastResetTime by PlaceHold.reference<Date>("state.startTime")
subVote("快速出波(默认10波,最高50)", "[波数]", "skipWave", "跳波") {
    val t = min(arg.firstOrNull()?.toIntOrNull() ?: 10, 50)
    VoteHandler.start(player!!, "跳波({t}波)".with("t" to t), supportSingle = true) {
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
subVote("回滚到某个存档(使用/slots查看)", "<存档ID>", "rollback", "load", "回档") {
    if (arg.firstOrNull()?.toIntOrNull() == null)
        return@subVote reply("[red]请输入正确的存档编号".with())
    val map = SharedData.mapManager.getSlot(arg[0].toInt())
            ?: return@subVote reply("[red]存档不存在或存档损坏".with())
    VoteHandler.start(player!!, "回档".with(), supportSingle = true) {
        SharedData.mapManager.loadSave(map)
        broadcast("[green]回档成功".with(), quite = true)
    }
}
subVote("踢出某人15分钟", "<玩家名>", "kick", "踢出") {
    val target = playerGroup.find { it.name == arg.joinToString(" ") }
            ?: return@subVote reply("[red]请输入正确的玩家名，或者到列表点击投票".with())
    if (SharedData.admin.isAdmin(player!!))
        return@subVote SharedData.admin.ban(player!!, target.uuid)
    VoteHandler.start(player!!, "踢人(踢出[red]{target.name}[yellow])".with("target" to target)) {
        if (SharedData.admin.isAdmin(target)) {
            return@start broadcast("[red]错误: {target.name}[red]为管理员, 如有问题请与服主联系".with("target" to target))
        }
        target.info.lastKicked = Time.millis() + (15 * 60 * 1000) //Kick for 15 Minutes
        target.con?.kick("[yellow]你被投票踢出15分钟")
        SharedData.admin.secureLog("Kick", "${target.name}(${target.uuid},${target.con.address}) is kicked By ${player!!.name}(${player!!.uuid})")
    }
}
subVote("清理本队建筑记录", "", "clear", "清理", "清理记录") {
    val team = player!!.team
    VoteHandler.apply {
        canVote = canVote.let { default -> { default(it) && it.team == team } }
        requireNum = { ceil(allCanVote().size * 2.0 / 5).toInt() }
        start(player!!, "清理建筑记录({team.colorizeName}[yellow]队|需要2/5同意)".with("team" to team)) {
            team.data().brokenBlocks.clear()
        }
    }
}

//instead of object because object can't be inner
@Suppress("PropertyName")
val VoteHandler = VoteHandler()

inner class VoteHandler {
    //private set
    val voting = AtomicBoolean(false)
    private val voted: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private var lastAction = 0L //最后一次玩家退出或投票成功时间,用于处理单人投票

    lateinit var voteDesc: PlaceHoldContext
    lateinit var requireNum: () -> Int
    lateinit var canVote: (Player) -> Boolean

    init {
        reset()
    }

    fun start(player: Player, voteDesc: PlaceHoldContext, supportSingle: Boolean = false, onSuccess: () -> Unit) {
        if (voting.get()) return
        voting.set(true)
        this.voteDesc = voteDesc
        GlobalScope.launch {
            try {
                if (supportSingle && allCanVote().run { count(canVote) == 0 || singleOrNull() == player }) {
                    if (System.currentTimeMillis() - lastAction > 60_000) {
                        broadcast("[yellow]单人快速投票{type}成功".with("type" to voteDesc))
                        lastAction = System.currentTimeMillis()
                        Core.app.post(onSuccess)
                        return@launch
                    } else
                        broadcast("[red]距离上一玩家离开或上一投票成功不足1分钟,快速投票失败".with())
                }
                broadcast("[yellow]{player.name}[yellow]发起{type}[yellow]投票,共需要{require}人,输入y或1同意"
                        .with("player" to player, "require" to requireNum(), "type" to voteDesc))
                repeat(voteTime.seconds.toInt()) {
                    delay(1000L)
                    if (voted.size >= requireNum()) {//提前结束
                        broadcast("[yellow]{type}[yellow]投票结束,投票成功.[green]{voted}/{all}[yellow],达到[red]{require}[yellow]人"
                                .with("type" to voteDesc, "voted" to voted.size, "require" to requireNum(), "all" to allCanVote().count(canVote)))
                        Core.app.post(onSuccess)
                        return@launch
                    }
                }
                //TimeOut
                broadcast("[yellow]{type}[yellow]投票结束,投票失败.[green]{voted}/{all}[yellow],未达到[red]{require}[yellow]人"
                        .with("type" to voteDesc, "voted" to voted.size, "require" to requireNum(), "all" to allCanVote().count(canVote)))
            } finally {
                reset()
            }
        }
    }

    fun allCanVote() = playerGroup.filter(canVote)

    private fun reset() {
        requireNum = { max(ceil(allCanVote().size * 2.0 / 3).toInt(), 2) }
        canVote = {
            val active = depends("wayzer/user/statistics")?.import<(Player) -> Boolean>("active") ?: { true }
            !it.dead && active(it)
        }
        voted.clear()
        voting.set(false)
    }

    fun onVote(p: Player) {
        if (!voting.get()) return
        if (p.uuid in voted) return p.sendMessage("[red]你已经投票".with())
        if (!canVote(p)) return p.sendMessage("[red]你不能对此投票".with())
        voted.add(p.uuid)
        broadcast("[green]投票成功,还需{left}人投票".with("left" to (requireNum() - voted.size)), quite = true)
    }

    fun onLeave(p: Player) {
        lastAction = System.currentTimeMillis()
        voted.remove(p.uuid)
    }
}

listen<EventType.PlayerChatEvent> { e ->
    if (e.message.equals("y", true) || e.message == "1") VoteHandler.onVote(e.player)
}

listen<EventType.PlayerJoin> {
    if (!VoteHandler.voting.get()) return@listen
    it.player.sendMessage("[yellow]当前正在进行{type}[yellow]投票，输入y或1同意".with("type" to VoteHandler.voteDesc))
}

listen<EventType.PlayerLeave> {
    VoteHandler.onLeave(it.player)
}