@file:Depends("wayzer/vote", "投票实现")
@file:Depends("wayzer/user/ban", "禁封实现")
@file:Depends("wayzer/cmds/share")

package wayzer.cmds

import wayzer.VoteEvent
import wayzer.user.BanService


val banService by Services.get<BanService>().notNull
command("kick", "踢出某人".with(), commands = VoteEvent.VoteCommands) {
    aliases = listOf("踢出")
    usage = "<玩家名/id> <理由>"
    requirePermission("wayzer.vote.kick")
    body {
        val target = getTarget()
        val reason = getInput("踢人理由", "[red]投票踢人需要理由".with())
        val player = player!!
        val event = VoteEvent(
            thisScript, player,
            voteDesc = "踢人(踢出[red]{target}[yellow])".with("target" to target),
            extDesc = "[red]理由: [yellow]${reason}"
        )
        val snapshot = PlayerData[target]
        if (event.awaitResult()) {
            if (target.hasPermission("wayzer.admin.skipKick"))
                return@body broadcast(
                    "[red]错误: {target.name}[red]为管理员, 如有问题请与服主联系".with("target" to target)
                )
            banService.ban(snapshot, 60, "投票踢出: $reason", player)
        }
    }
}

PermissionApi.registerDefault("wayzer.admin.skipKick", group = "@admin")