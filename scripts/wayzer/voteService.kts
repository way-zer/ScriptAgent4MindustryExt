@file:Depends("wayzer/user/ext/activeCheck", "玩家活跃判定", soft = true)

package wayzer

import cf.wayzer.placehold.PlaceHoldContext
import mindustry.gen.Groups
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.max

name = "投票服务"

val voteTime by config.key(Duration.ofSeconds(60)!!, "投票时间")

protected val Player.active
    get() = textFadeTime >= 0 || depends("wayzer/user/ext/activeCheck")
        ?.import<(Player) -> Int>("inactiveTime")
        ?.let { it(this) < 30_000 } ?: true

//public
val voteCommands: Commands = VoteCommands()
lateinit var requireNum: () -> Int
lateinit var canVote: (Player) -> Boolean
//private
protected val voting = AtomicBoolean(false)
protected val voted: MutableSet<String> = ConcurrentHashMap.newKeySet()
protected var lastAction = 0L //最后一次玩家退出或投票成功时间,用于处理单人投票
protected lateinit var voteDesc: PlaceHoldContext

fun allCanVote() = Groups.player.filter(canVote)
fun start(player: Player, voteDesc: PlaceHoldContext, supportSingle: Boolean = false, onSuccess: () -> Unit) {
    if (voting.get()) return
    voting.set(true)
    this.voteDesc = voteDesc
    launch(Dispatchers.game) {
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
            broadcast(
                "[yellow]{player.name}[yellow]发起{type}[yellow]投票,共需要{require}人,输入y或1同意"
                    .with("player" to player, "require" to requireNum(), "type" to voteDesc)
            )
            repeat(voteTime.seconds.toInt()) {
                delay(1000L)
                if (voted.size >= requireNum()) {//提前结束
                    broadcast(
                        "[yellow]{type}[yellow]投票结束,投票成功.[green]{voted}/{all}[yellow],达到[red]{require}[yellow]人"
                            .with(
                                "type" to voteDesc,
                                "voted" to voted.size,
                                "require" to requireNum(),
                                "all" to allCanVote().count(canVote)
                            )
                    )
                    Core.app.post(onSuccess)
                    return@launch
                }
            }
            //TimeOut
            broadcast(
                "[yellow]{type}[yellow]投票结束,投票失败.[green]{voted}/{all}[yellow],未达到[red]{require}[yellow]人"
                    .with(
                        "type" to voteDesc,
                        "voted" to voted.size,
                        "require" to requireNum(),
                        "all" to allCanVote().count(canVote)
                    )
            )
        } finally {
            reset()
        }
    }
}

fun ISubScript.addSubVote(
    desc: String,
    usage: String,
    vararg aliases: String,
    body: CommandContext.() -> Unit
) {
    voteCommands += CommandInfo(this, aliases.first(), desc) {
        this.usage = usage
        this.aliases = aliases.toList()
        body(body)
        if (permission.isEmpty())
            permission = "wayzer.vote." + aliases.first().toLowerCase()
    }
    voteCommands.autoRemove(this)
}

//private
fun reset() {
    requireNum = { max(ceil(allCanVote().size * 2.0 / 3).toInt(), 2) }
    canVote = { !it.dead() && it.active }
    voted.clear()
    voting.set(false)
}
onEnable { reset() }
fun onVote(p: Player) {
    if (!voting.get()) return
    if (p.uuid() in voted) return p.sendMessage("[red]你已经投票".with())
    if (!canVote(p)) return p.sendMessage("[red]你不能对此投票".with())
    voted.add(p.uuid())
    broadcast("[green]投票成功,还需{left}人投票".with("left" to (requireNum() - voted.size)), quite = true)
}
listen<EventType.PlayerChatEvent> { e ->
    e.player.textFadeTime = 0f //防止因为不说话判定为挂机
    if (e.message.equals("y", true) || e.message == "1") {
        onVote(e.player)
    }
}

listen<EventType.PlayerJoin> {
    if (!voting.get()) return@listen
    it.player.sendMessage("[yellow]当前正在进行{type}[yellow]投票，输入y或1同意".with("type" to voteDesc))
}

listen<EventType.PlayerLeave> {
    lastAction = System.currentTimeMillis()
    voted.remove(it.player.uuid())
}

inner class VoteCommands : Commands() {
    override fun invoke(context: CommandContext) {
        if (voting.get()) return context.reply("[red]投票进行中".with())
        super.invoke(context)
        if (voting.get()) {//success
            Call.sendMessage(
                context.prefix + context.arg.joinToString(" "),
                mindustry.core.NetClient.colorizeName(context.player!!.id, context.player!!.name),
                context.player!!
            )
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

command("vote", "发起投票") {
    type = CommandType.Client
    aliases = listOf("投票")
    body(voteCommands)
}
command("votekick", "(弃用)投票踢人") {
    this.usage = "<player...>";this.type = CommandType.Client
    body {
        //Redirect
        arg = listOf("kick", *arg.toTypedArray())
        voteCommands.invoke(this)
    }
}