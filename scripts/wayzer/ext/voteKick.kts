@file:Depends("wayzer/vote", "投票实现")
@file:Depends("wayzer/user/ban", "禁封实现", soft = true)
@file:Depends("coreMindustry/utilTextInput", "输入理由")
@file:Depends("coreMindustry/menu", "菜单选人")

package wayzer.ext

import arc.util.Time
import coreMindustry.PagedMenuBuilder
import wayzer.VoteEvent

fun CommandContext.readArg(): String? = arg.firstOrNull().also { arg = arg.drop(1) }

suspend fun CommandContext.getTarget(): Player {
    val id = readArg() ?: player?.let { player ->
        var result: Player? = null
        PagedMenuBuilder(Groups.player.toList()) {
            option(it.name) { result = it }
        }.apply {
            title = "选择目标玩家"
            sendTo(player, 60_000)
        }
        if (result != null) return result!!
        null
    } ?: returnReply("[red]请输入玩家名/三位id".with())
    if (id.startsWith("#"))
        Groups.player.getByID(id.substring(1).toIntOrNull() ?: 0)?.let { return it }
    //Try find by name
    val allPlayers = Groups.player.associateBy { it.name.replace(" ", "") }
    for (addLen in 0..arg.size) {
        val argAsName = id + arg.take(addLen).joinToString("")
        val found = allPlayers[argAsName] ?: continue
        arg = arg.drop(addLen)
        return found
    }
    //find by uuid
    return thisContextScript().depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")
        ?.invoke(id)?.let { uuid -> Groups.player.find { it.uuid() == uuid } }
        ?: returnReply("[red]请输入正确的玩家名".with())
}

val textInput = contextScript<coreMindustry.UtilTextInput>()
suspend fun CommandContext.getInput(name: String, whenEmpty: PlaceHoldString): String {
    return arg.takeIf { it.isNotEmpty() }?.joinToString(" ")
        ?: player?.let { p ->
            (textInput.textInput(p, "请在60s内输入$name") ?: returnReply("[yellow]已取消输入".with()))
                .takeIf { it.isNotBlank() }
        } ?: returnReply(whenEmpty)
}

onEnable {
    val script = this
    VoteEvent.VoteCommands += CommandInfo(this, "kick", "踢出某人") {
        aliases = listOf("踢出")
        usage = "<玩家名/id> <理由>"
        permission = "wayzer.vote.kick"
        body {
            val target = getTarget()
            val reason = getInput("踢人理由", "[red]投票踢人需要理由".with())
            val player = player!!
            val event = VoteEvent(
                script, player,
                voteDesc = "踢人(踢出[red]{target}[yellow])".with("target" to target),
                extDesc = "[red]理由: [yellow]${reason}"
            )
            if (event.awaitResult()) {
                if (target.hasPermission("wayzer.admin.skipKick"))
                    return@body broadcast(
                        "[red]错误: {target.name}[red]为管理员, 如有问题请与服主联系".with("target" to target)
                    )
                depends("wayzer/user/ban")?.import<(String, Int, String, PlayerProfile?) -> Unit>("ban")
                    ?.invoke(target.uuid(), 60, "投票踢出: $reason", PlayerData[player.uuid()].profile)
                    ?: run {
                        target.info.lastKicked = Time.millis() + (15 * 60 * 1000) //Kick for 15 Minutes
                        target.con?.kick("[yellow]你被投票踢出15分钟")
                    }
            }
        }
    }
}

command("votekick", "(弃用)投票踢人") {
    this.usage = "<player...>";this.type = CommandType.Client
    body {
        //Redirect
        arg = listOf("kick", *arg.toTypedArray())
        VoteEvent.VoteCommands.invoke(this)
    }
}
PermissionApi.registerDefault("wayzer.admin.skipKick", group = "@admin")