@file:Depends("wayzer/cmds/voteKick", "功能控制，使用util，覆盖votekick")
@file:Depends("wayzer/map/betterTeam", "强制观察者")

package wayzer.cmds

import wayzer.VoteEvent
import wayzer.map.BetterTeam
import java.time.Duration
import java.time.Instant

val teams = contextScript<BetterTeam>()
val voteKick = contextScript<VoteKick>()

@Savable(false)
val limitPlayers = mutableMapOf<String, Pair<String, Instant>>()//profile -> reason,time
customLoad(this::limitPlayers) { limitPlayers.putAll(it) }

onEnable {
    val script = this
    VoteEvent.VoteCommands += CommandInfo(script, "ob", "强制观战") {
        aliases = listOf("观战")
        usage = "<玩家名/id> <理由>"
        permission = "wayzer.vote.ob"
        body {
            val target = with(voteKick) { getTarget() }
            val reason = with(voteKick) { getInput("限制观战理由", "[red]投票限制他人需要理由".with()) }
            val player = player!!
            val event = VoteEvent(
                script, player,
                voteDesc = "强制观战(目标[red]{target.name}[yellow])".with("target" to target),
                extDesc = "[red]理由: [yellow]${reason}"
            )
            val ids = PlayerData[target].ids
            if (event.awaitResult()) {
                if (target.hasPermission("wayzer.admin.skipKick"))
                    return@body broadcast(
                        "[red]错误: {target.name}[red]为管理员, 如有问题请与服主联系".with("target" to target)
                    )
                ids.forEach {
                    limitPlayers[it] = reason to Instant.now()
                }
                teams.changeTeam(target, teams.spectateTeam)
                broadcast(
                    "[yellow][提示][green]如目标用户继续捣乱，可以使用[gold]/vote kick {player.shortID}[]投票踢出".with(
                        "player" to target
                    )
                )
            }
        }
    }
    VoteEvent.VoteCommands += CommandInfo(script, "quitOb", "解除强行观战限制(限本人)") {
        aliases = listOf("解除观战")
        body {
            val player = player!!
            val id = PlayerData[player].id
            val (reason, time) = limitPlayers[id]
                ?: returnReply("[yellow]你未被限制游戏，无需解除".with())
            val delta = Duration.between(time, Instant.now())
            val event = VoteEvent(
                script, player,
                voteDesc = "解除强制(已持续{delta 分钟})".with("delta" to delta),
                extDesc = "[yellow]被限制时的理由: $reason"
            )
            if (event.awaitResult()) {
                limitPlayers.remove(id)
                teams.changeTeam(player)
            }
        }
    }
}

listenTo<BetterTeam.AssignTeamEvent>(Event.Priority.Intercept) {
    limitPlayers[PlayerData[player].id]?.let { (reason, time) ->
        val delta = Duration.between(time, Instant.now())
        player.sendMessage(
            """
                [red]你已被限制强制观战.
                [yellow]投票原因: [white]{reason}({delta 分钟}前)
                [yellow]如有疑问，请在聊天区交流
                [green]可通过[gold]/vote quitOb[]投票，取消限制
            """.trimIndent().with("reason" to reason, "delta" to delta),
            MsgType.InfoMessage
        )
        team = teams.spectateTeam
    }
}
command("votekick", "(弃用)投票踢人") {
    this.usage = "<player...>"
    attr(ClientOnly)
    body {
        //Redirect
        arg = listOf("ob", *arg.toTypedArray())
        VoteEvent.VoteCommands.handle()
    }
}
command("forceOB", "管理指令：使某人强制观战") {
    usage = "<玩家名/id>"
    permission = "wayzer.admin.forceOb"
    body {
        val target = with(voteKick) { getTarget() }
        val id = PlayerData[target].id
        if (id in limitPlayers) {
            limitPlayers.remove(id)
            teams.changeTeam(target)
            returnReply("[green]已解除目标限制".with())
        }
        val reason = with(voteKick) { getInput("限制观战理由", "[red]投票限制他人需要理由".with()) }
        limitPlayers[id] = reason to Instant.now()
        teams.changeTeam(target, teams.spectateTeam)
        broadcast(
            "[red] 管理员强制{target.name}[red]成为观察者,原因: [yellow]{reason}"
                .with("target" to target, "reason" to reason)
        )
    }
}
PermissionApi.registerDefault("wayzer.admin.forceOb", group = "@admin")