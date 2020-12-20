@file:Depends("wayzer/user/statistics", "玩家活跃判定", soft = true)
@file:Import("@wayzer/services/VoteService.kt", sourceFile = true)

package wayzer

import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.script_agent.util.ServiceRegistry
import coreMindustry.lib.util.sendMenuPhone
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.gen.Call
import wayzer.services.VoteService
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.max

name = "投票服务"

val voteTime by config.key(Duration.ofSeconds(60)!!, "投票时间")

inner class VoteCommands : Commands() {
    override fun invoke(context: CommandContext) {
        if (VoteHandler.voting.get()) return context.reply("[red]投票进行中".with())
        super.invoke(context)
        if (VoteHandler.voting.get()) {//success
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

//instead of object because object can't be inner
@Suppress("PropertyName")
val VoteHandler = VoteHandler()

inner class VoteHandler : VoteService {
    override val voteCommands: Commands = VoteCommands()

    //private set
    val voting = AtomicBoolean(false)
    private val voted: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private var lastAction = 0L //最后一次玩家退出或投票成功时间,用于处理单人投票

    lateinit var voteDesc: PlaceHoldContext
    override lateinit var requireNum: () -> Int
    override lateinit var canVote: (Player) -> Boolean

    init {
        reset()
    }

    override fun start(player: Player, voteDesc: PlaceHoldContext, supportSingle: Boolean, onSuccess: () -> Unit) {
        if (voting.get()) return
        voting.set(true)
        this.voteDesc = voteDesc
        GlobalScope.launch(Dispatchers.game) {
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

    override fun allCanVote() = playerGroup.filter(canVote)

    private fun reset() {
        requireNum = { max(ceil(allCanVote().size * 2.0 / 3).toInt(), 2) }
        canVote = {
            val active = depends("wayzer/user/statistics")?.import<(Player) -> Boolean>("active") ?: { true }
            !it.dead() && active(it)
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

    override fun ISubScript.addSubVote(
        desc: String,
        usage: String,
        vararg aliases: String,
        body: CommandContext.() -> Unit
    ) {
        voteCommands += CommandInfo(this, aliases.first(), desc) {
            this.usage = usage
            this.aliases = aliases.toList()
            body(body)
        }
        voteCommands.autoRemove(this)
    }
}

provide<VoteService>(VoteHandler)

command("vote", "发起投票") {
    type = CommandType.Client
    aliases = listOf("投票")
    body(VoteHandler.voteCommands)
}
command("votekick", "(弃用)投票踢人") {
    this.usage = "<player...>";this.type = CommandType.Client
    body {
        //Redirect
        arg = listOf("kick", *arg.toTypedArray())
        VoteHandler.voteCommands.invoke(this)
    }
}

listen<EventType.PlayerChatEvent> { e ->
    e.player.textFadeTime = 0f //防止因为不说话判定为挂机
    if (e.message.equals("y", true) || e.message == "1") VoteHandler.onVote(e.player)
}

listen<EventType.PlayerJoin> {
    if (!VoteHandler.voting.get()) return@listen
    it.player.sendMessage("[yellow]当前正在进行{type}[yellow]投票，输入y或1同意".with("type" to VoteHandler.voteDesc))
}

listen<EventType.PlayerLeave> {
    VoteHandler.onLeave(it.player)
}