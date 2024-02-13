package wayzer

import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.contextScript
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.emitAsync
import coreLibrary.lib.*
import coreMindustry.MenuBuilder
import coreMindustry.lib.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import mindustry.gen.Groups
import mindustry.gen.Player
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.ceil

@Suppress("MemberVisibilityCanBePrivate")
class VoteEvent(
    scope: CoroutineScope,
    val starter: Player,
    val voteDesc: PlaceHoldContext,
    val extDesc: String = "",
    val supportSingle: Boolean = false,
    val canVote: (Player) -> Boolean = { !it.dead() },
    val requireNum: (all: Int) -> Int = { ceil(it * 0.6).toInt() },
    val fastSuccess: Boolean = true,
) : Event, Event.Cancellable {
    enum class Action { Agree, Disagree, Ignore, Quit, Join }

    val voted = mutableMapOf<Player, Boolean?>()
    var succeed = false
    val endTime: Instant = Instant.now() + voteTime
    override var cancelled
        get() = !mainJob.isActive
        set(value) {
            if (value) mainJob.cancel()
        }

    suspend fun awaitResult(): Boolean {
        mainJob.join()
        return succeed
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val mainJob = scope.launch(Dispatchers.game + CoroutineName("Vote Service"), CoroutineStart.LAZY) main@{
        if ((coolDowns[starter.uuid()] ?: 0) > System.currentTimeMillis()) {
            starter.sendMessage("[yellow]你刚发起的投票投票失败，投票冷却中".with())
            return@main
        }
        emitAsync {
            if (supportSingle && allCanVote().run { isEmpty() || singleOrNull() == starter }) {
                if (System.currentTimeMillis() - lastAction > 60_000) {
                    broadcast("[yellow]单人快速投票{type}成功".with("type" to voteDesc))
                    lastAction = System.currentTimeMillis()
                    succeed = true
                    return@emitAsync
                } else broadcast("[red]距离上一玩家离开或上一投票成功不足1分钟,快速投票失败".with())
            }
            if (!active.compareAndSet(null, this@VoteEvent)) {
                return@emitAsync cancel()
            }
            launch {
                try {
                    awaitCancellation()
                } finally {
                    active.compareAndSet(this@VoteEvent, null)
                }
            }
            //文字投票
            val delayTip = if (menuDelay <= 0) "".asPlaceHoldString()
            else "\n[green] (若未投票,{delay}秒后将弹窗提示)".with("delay" to menuDelay)
            val tip = """
                [yellow]{starter.name}[yellow]发起{type}[yellow]投票
                {ext}
                [yellow]你可以在投票结束前使用文字投票，[green]赞成(y/1)[][yellow]中立(.)[][red]反对(n/0)[]投票{delayTip}
                """.trimIndent()
                .with("starter" to starter, "type" to voteDesc, "ext" to extDesc, "delayTip" to delayTip)
            broadcast(tip)

            broadcast(tip, type = MsgType.Announce, players = allCanVote(), quite = true)
            vote(starter, Action.Agree)
            //弹窗投票
            if (menuDelay >= 0) allCanVote().forEach {
                launch(Dispatchers.game) {
                    delay(menuDelay * 1000L)
                    if (it in voted) return@launch
                    openMenu(it)
                }
            }
            //投票超时处理
            val actionHandler = launch { actionHandler() }
            select {
                actionHandler.onJoin {}
                onTimeout(voteTime.toMillis()) {
                    actionHandler.cancel()
                    withCheckVoted {
                        val minVoteNum = allCanVote().size / 2
                        if (voted.size < minVoteNum && agree() < requireNum(minVoteNum)) {
                            broadcast("[yellow]投票参与人数过少".with())
                        } else {
                            succeed = agree() >= requireNum(agree() + disagree()).coerceAtLeast(1)
                        }
                    }
                }
            }

            if (!succeed) coolDowns[starter.uuid()] = System.currentTimeMillis() + voteCoolDown.toMillis()
            val t = if (succeed) "[yellow]{starter.name}[yellow]发起{type}[yellow]投票成功. {status}"
            else "[yellow]{starter.name}[yellow]发起{type}[yellow]投票失败. {status}"
            broadcast(t.with("starter" to starter, "type" to voteDesc, "status" to status()))
        }
        coroutineContext.cancelChildren()
    }

    fun allCanVote() = Groups.player.filter(canVote)
    fun agree() = voted.count { it.value == true }
    fun middle() = voted.count { it.value == null }
    fun disagree() = voted.count { it.value == false }
    fun notVote() = allCanVote().size - voted.size
    fun status() = withCheckVoted {
        "[green]\uE804${agree()} [yellow]\uE853${middle()} [red]\uE805${disagree()} [grey]\uE88F${notVote()}"
    }

    inline fun <T> withCheckVoted(body: () -> T): T {
        voted.entries.removeIf { !canVote(it.key) }
        return body()
    }

    fun vote(p: Player, action: Action) {
        handleAction.trySend(p to action)
    }

    suspend fun openMenu(p: Player) {
        MenuBuilder<Unit>("投票") {
            msg = "[yellow]{starter.name}[yellow]发起{type}[yellow]投票\n{ext}".with(
                "starter" to starter,
                "type" to voteDesc,
                "ext" to extDesc
            ).toPlayer(p)
            option("赞成") { vote(p, Action.Agree) }
            option("中立") { vote(p, Action.Ignore) }
            option("反对") { vote(p, Action.Disagree) }
            newRow()
            option("待定") {
                p.sendMessage("[yellow]将在20s后再次提示")
                script.launch(Dispatchers.game) {
                    delay(20_000)
                    if (active.get() == this@VoteEvent && p !in voted) openMenu(p)
                }
            }
        }.sendTo(p, 60_000)
    }

    private val handleAction = Channel<Pair<Player, Action>>(Channel.UNLIMITED)
    private suspend fun actionHandler() = coroutineScope {
        for ((player, event) in handleAction) {
            when (event) {
                Action.Join -> if (this@VoteEvent.canVote(player)) {
                    launch(Dispatchers.gamePost) { openMenu(player) }
                }

                Action.Quit -> voted.remove(player)
                Action.Agree, Action.Disagree, Action.Ignore -> {
                    if (!this@VoteEvent.canVote(player)) {
                        player.sendMessage("[red]你不能对此投票".with())
                        continue
                    }
                    voted[player] = when (event) {
                        Action.Agree -> true
                        Action.Disagree -> false
                        else -> null
                    }
                    player.sendMessage("[green]投票成功".with())
                }
            }

            //fast path
            withCheckVoted {
                val all = allCanVote().size - middle()
                when {
                    fastSuccess && agree() >= this@VoteEvent.requireNum(all) -> {
                        succeed = true;return@coroutineScope
                    }

                    all - disagree() < this@VoteEvent.requireNum(all) -> {
                        succeed = false;return@coroutineScope
                    }

                    else -> {}
                }
            }
        }
        handleAction.close()
    }

    init {
        mainJob.start()
    }

    object VoteCommands : Commands() {
        override suspend fun invoke(context: CommandContext) {
            if (active.get() != null) return context.reply("[red]投票进行中".with())
            super.invoke(context)
        }
    }

    companion object : Event.Handler() {
        internal val script = contextScript<Vote>()
        private val voteTime by script.config.key(Duration.ofSeconds(60)!!, "投票时间")
        private val voteCoolDown by script.config.key(Duration.ofMinutes(5)!!, "投票失败冷却时间")
        private val menuDelay by script.config.key(20, "弹窗投票显示时间,单位秒", "0为立即显示，-1纯文字投票")

        internal val active = AtomicReference<VoteEvent?>(null)
        internal var lastAction = 0L //最后一次玩家退出或投票成功时间,用于处理单人投票
        public val coolDowns = mutableMapOf<String, Long>()
    }
}

@Deprecated("use VoteEvent")
object VoteService {
    @ScriptDsl
    fun Script.addSubVote(
        desc: String, usage: String, vararg aliases: String, body: suspend CommandContext.() -> Unit
    ) {
        VoteEvent.VoteCommands += CommandInfo(this, aliases.first(), desc.with()) {
            this.usage = usage
            this.aliases = aliases.toList()
            body(body)
            if (permission.isEmpty()) permission = "wayzer.vote." + aliases.first().lowercase()
        }
    }

    fun start(
        starter: Player,
        voteDesc: PlaceHoldContext,
        extDesc: String = "",
        supportSingle: Boolean = false,
        canVote: (Player) -> Boolean = { !it.dead() },
        requireNum: (all: Int) -> Int = { ceil(it * .6).toInt() },
        fastSuccess: Boolean = true,
        onSuccess: suspend (Map<Player, Boolean?>) -> Unit
    ) {
        VoteEvent.script.launch(Dispatchers.game) {
            val event = VoteEvent(this, starter, voteDesc, extDesc, supportSingle, canVote, requireNum, fastSuccess)
            if (event.awaitResult()) onSuccess(event.voted)
        }
    }
}