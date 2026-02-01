package wayzer.cmds

import cf.wayzer.placehold.VarString
import cf.wayzer.scriptAgent.define.Script
import coreLibrary.lib.CommandContext
import coreLibrary.lib.returnReply
import coreLibrary.lib.with
import coreMindustry.PagedMenuBuilder
import coreMindustry.lib.player
import coreMindustry.util.textInput
import mindustry.gen.Groups
import mindustry.gen.Player
import wayzer.lib.PlayerData

fun CommandContext.readArg(): String? = arg.firstOrNull().also { arg = arg.drop(1) }

suspend fun CommandContext.getTarget(): Player {
    //1. by Arg
    readArg()?.let { id ->
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
        return PlayerData.findByShortId(id)?.player
            ?: returnReply("[red]请输入正确的玩家名".with())

    }
    //2. use menu
    player?.let { player ->
        var result: Player? = null
        PagedMenuBuilder(Groups.player.toList()) {
            option(it.name) { result = it }
        }.apply {
            title = "选择目标玩家"
            sendTo(player, 60_000)
        }
        result?.let { return it }
    }
    returnReply("[red]请输入玩家名/三位id".with())
}

context(_: Script)
suspend fun CommandContext.getInput(name: String, whenEmpty: VarString): String {
    return arg.takeIf { it.isNotEmpty() }?.joinToString(" ")
        ?: player?.let { p ->
            (textInput(p, "请在60s内输入$name") ?: returnReply("[yellow]已取消输入".with()))
                .takeIf { it.isNotBlank() }
        } ?: returnReply(whenEmpty)
}