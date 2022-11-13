@file:Depends("wayzer/user/ext/activeCheck", "玩家活跃判定", soft = true)
@file:Depends("coreMindustry/utilNext", "调用菜单")

package wayzer

import arc.Core
import cf.wayzer.placehold.PlaceHoldContext
import coreLibrary.lib.*
import coreMindustry.lib.*
import coreMindustry.lib.util.sendMenuPhone
import mindustry.Vars.netServer
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Player
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

name = "投票服务"

val voteTime by config.key(Duration.ofSeconds(60)!!, "投票时间")

//bundle
//single player vote
val singleVoteDone = "[violet][投票系统][] [yellow]单人快速投票{type}通过"
val singleVoteFail = "[violet][投票系统][] [red]距离上一玩家离开或上一投票成功不足1分钟,快速投票失败"
//voting tip
val voteStart = ("[violet][投票系统][] [yellow]{player.name}[yellow]发起{type}[yellow]投票,持续{time}秒.\n" +
        "[violet][投票系统][] [yellow]赞成人数大于反对人数的两倍则通过\n" +
        "[violet][投票系统][] [yellow]在聊天栏输入y或1赞成,输入0或n反对")
val voteFinish = "[violet][投票系统][] [yellow]{type}[yellow]投票{result}.[green]赞成{agreed}[]/[red]反对{disagreed}[]/[purple]中立{abstained}[]"
val voteTip = ("[violet][投票系统][] [yellow]{type}[yellow]投票还剩[cyan]{seconds}[yellow]秒,当前[green]赞成{agreed}[]/[red]反对{disagreed}[]/[purple]中立{abstained}[].\n" +
        "[violet][投票系统][] [yellow]赞成人数大于反对人数的两倍则通过\n" +
        "[violet][投票系统][] [yellow]还没有投票的可以在聊天栏输入y或1赞成,输入0或n反对")
val voted = "[violet][投票系统][] {name} [lightgray]已{text},当前[green]{agreed}[]/[red]{disagreed}[]/[purple]{abstained}[]"
val abstainTip = "[violet][投票系统][] [yellow]在聊天栏输入y或1同意, 输入n或0反对"
val joinTip = "[yellow]当前正在进行{type}[yellow]投票,[yellow]输入y或1同意, 输入n或0反对"
val playerNotEnough = "[violet][投票系统][] [red]投票参与人数过少，需要1/4参与[yellow](当前{voted}票/{all}人)"
//UI Display
val UITitle = "投票系统 by cong"
val UIText = "{votePlayer}发起了{type}[yellow]投票\n[yellow]赞成人数大于反对人数的两倍则通过"
//voteResult
val voteDone = "[green]通过[]"
val voteFail = "[red]未通过[]"
//UIbotton
val agree = "[green]赞成[]"
val disAgree = "[red]反对[]"
val wait = "[lightgray]等下再投票"
//warning
val alreadyFinish = "[red]投票已经结束"
val alreadyAgree = "[red]你已经赞成"
val alreadyDisAgree = "[red]你已经反对"
val cannotVote = "[red]你不能对此投票"
val Voting = "[red]投票进行中"
val typeError = "[red]错误投票类型,请检查输入是否正确"
val voteType = "可用投票类型"

protected val Player.active
    get() = textFadeTime >= 0 || depends("wayzer/user/ext/activeCheck")
        ?.import<(Player) -> Int>("inactiveTime")
        ?.let { it(this) < 30_000 } ?: true

//public
val voteCommands: Commands = VoteCommands()
lateinit var voteScore: () -> Int
lateinit var canVote: (Player) -> Boolean
//private
protected val voting = AtomicBoolean(false)
protected val agreed: MutableSet<String> = ConcurrentHashMap.newKeySet()
protected val disagreed: MutableSet<String> = ConcurrentHashMap.newKeySet()
protected val menu = contextScript<coreMindustry.UtilNext>()
protected var votePlayerName = ""
protected var lastAction = 0L //最后一次玩家退出或投票成功时间,用于处理单人投票
protected lateinit var voteDesc: PlaceHoldContext

fun allCanVote() = Groups.player.filter(canVote)
fun abstained() = allCanVote().count(canVote) - agreed.size - disagreed.size;
fun start(player: Player, voteDesc: PlaceHoldContext, supportSingle: Boolean = false, onSuccess: () -> Unit) {
    if (voting.get()) return
    voting.set(true)
    this.voteDesc = voteDesc
    launch(Dispatchers.game) {
        try {
            votePlayerName = player.name
            if (supportSingle && allCanVote().run { count(canVote) == 0 || singleOrNull() == player }) {
                if (System.currentTimeMillis() - lastAction > 60_000) {
                    broadcast(singleVoteDone.with("type" to voteDesc))
                    lastAction = System.currentTimeMillis()
                    Core.app.post(onSuccess)
                    return@launch

                } else
                    broadcast(singleVoteFail.with())
            }
            broadcast(voteStart.with(
                "player" to player,
                "type" to voteDesc,
                "time" to voteTime.seconds
            ))
            allCanVote().forEach{launch{sendVoteMenu(it)}}
            fun voteClosed(result: String) {
                broadcast(voteFinish.with(
                    "result" to result,
                    "type" to voteDesc,
                    "agreed" to agreed.size,
                    "disagreed" to disagreed.size,
                    "abstained" to abstained()
                ))
            }
            repeat(voteTime.seconds.toInt() * 10) {
                delay(100L)
                if (voteScore() - abstained() * 2 > 0) {//剩下全部人投反对还能通过
                    voteClosed(voteDone)
                    Core.app.post(onSuccess)
                    return@launch
                }
                if (voteScore() + abstained() < 0) {//剩下全部人投赞成还不能通过
                    voteClosed(voteFail)
                    return@launch
                }
                if (it == voteTime.seconds.toInt() * 10 * 2 / 3)
                    broadcast(voteTip.with(
                        "type" to voteDesc,
                        "seconds" to (voteTime.seconds.toInt() / 3),
                        "agreed" to agreed.size,
                        "disagreed" to disagreed.size,
                        "abstained" to abstained()
                    ))
            }
            //TimeOut
            if (agreed.size + disagreed.size < Groups.player.size() * 0.25) {
                broadcast(playerNotEnough.with(
                    "voted" to agreed.size + disagreed.size,
                    "all" to Groups.player.size()
                ))
            }
            else if (voteScore() >= 0) {
                voteClosed(voteDone)
                Core.app.post(onSuccess)
            }
            else {
                voteClosed(voteFail)
            }
        } finally {
            reset()
        }
    }
}

fun Script.addSubVote(
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
            permission = "wayzer.vote." + aliases.first().lowercase()
    }
    voteCommands.autoRemove(this)
}

//private
fun reset() {
    voteScore = { agreed.size - disagreed.size * 2}
    canVote = { !it.dead() && it.active }
    voting.set(false)
    agreed.clear()
    disagreed.clear()
}
onEnable { reset() }

fun onVote(p: Player,text: String, f: () -> Boolean) {
    if (!voting.get()) return p.sendMessage(alreadyFinish.with())
    if (!canVote(p)) return p.sendMessage(cannotVote.with())
    f()
    broadcast(voted.with(
        "name" to p.name,
        "text" to text,
        "agreed" to agreed.size,
        "disagreed" to disagreed.size,
        "abstained" to abstained()
    ), quite = true)
}
fun agree(p: Player) {
    onVote(p, agree) {
        agreed.add(p.uuid())
        disagreed.remove(p.uuid())
    }
}
fun disagree(p: Player) {
    onVote(p, disAgree) {
        disagreed.add(p.uuid())
        agreed.remove(p.uuid())
    }
}
fun abstain(p: Player) {
    p.sendMessage(abstainTip)
}

suspend fun sendVoteMenu(p: Player) {
    menu.sendMenuBuilder<Unit>(
        p, 30_000, UITitle,UIText.with("player" to p, "type" to voteDesc, "votePlayer" to votePlayerName).toString()
    ) {
        add(listOf(
            agree to {agree(p)},
            disAgree to {disagree(p)}
        ))
        add(listOf(wait to {abstain(p)}))
    }
}

listen<EventType.PlayerChatEvent> { e ->
    e.player.textFadeTime = 0f //防止因为不说话判定为挂机
    if (e.message.equals("y", true) || e.message == "1") {agree(e.player)}
    if (e.message.equals("n", false) || e.message == "0") {disagree(e.player)}
}

listen<EventType.PlayerJoin> {
    if (!voting.get()) return@listen
    it.player.sendMessage(joinTip.with("type" to voteDesc))
    launch {
        sendVoteMenu(it.player)
    }
}

listen<EventType.PlayerLeave> {
    lastAction = System.currentTimeMillis()

    agreed.remove(it.player.uuid())
    disagreed.remove(it.player.uuid())
}

inner class VoteCommands : Commands() {
    override suspend fun invoke(context: CommandContext) {
        if (voting.get()) return context.reply(Voting.with())
        super.invoke(context)
        if (voting.get()) {//success
            val raw = context.prefix + context.arg.joinToString(" ")
            val msg = netServer.chatFormatter.format(context.player!!, raw)
            Call.sendMessage(msg, raw, context.player!!)
        }
    }

    override suspend fun onHelp(context: CommandContext, explicit: Boolean) {
        if (!explicit) context.reply(typeError.with())
        context.sendMenuPhone(voteType, subCommands.values.toSet().filter {
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
