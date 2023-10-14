@file:Depends("coreMindustry/menu")

package wayzer

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.scriptAgent.events.ScriptDisableEvent
import java.time.Duration
import java.time.Instant

name = "投票服务"

listen<EventType.PlayerJoin> {
    (VoteEvent.active.get() ?: return@listen)
        .vote(it.player, VoteEvent.Action.Join)
}

listen<EventType.PlayerLeave> {
    VoteEvent.lastAction = System.currentTimeMillis()
    (VoteEvent.active.get() ?: return@listen)
        .vote(it.player, VoteEvent.Action.Quit)
}

listen<EventType.PlayerChatEvent> {
    val action = when (it.message.lowercase()) {
        "赞成", "y", "1" -> VoteEvent.Action.Agree
        "反对", "n", "0" -> VoteEvent.Action.Disagree
        "中立", "." -> VoteEvent.Action.Ignore
        else -> return@listen
    }
    (VoteEvent.active.get() ?: return@listen it.player.sendMessage("[red]投票已结束"))
        .vote(it.player, action)
}

listen<EventType.ResetEvent> { VoteEvent.coolDowns.clear() }

registerVar("scoreBroad.ext.vote", "投票状态显示", DynamicVar.v {
    VoteEvent.active.get()?.run {
        "[violet]投票[orange]{desc}: {status} [violet]\uE867{left:秒}".with(
            "desc" to voteDesc,
            "status" to status(),
            "left" to Duration.between(Instant.now(), endTime)
        )
    }
})

command("vote", "发起投票") {
    type = CommandType.Client
    aliases = listOf("投票")
    body(VoteEvent.VoteCommands)
}
listenTo<ScriptDisableEvent> {
    VoteEvent.VoteCommands.removeAll(script)
}
PermissionApi.registerDefault("wayzer.vote.*")