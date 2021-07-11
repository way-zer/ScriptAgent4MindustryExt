package wayzer.user

import mindustry.gen.Groups
import mindustry.net.Administration

val players = mutableSetOf<String>()

command("mute", "管理指令: 禁言") {
    usage = "<3位id>"
    permission = "wayzer.admin.mute"
    body {
        if (arg.size < 3) replyUsage()
        val uuid = netServer.admins.getInfoOptional(arg[0])?.id
            ?: depends("wayzer/admin")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(arg[0])
            ?: returnReply("[red]请输入目标3位ID,不清楚可通过/list查询".with())
        val target = Groups.player.find { it.uuid() == uuid } ?: null
        if (players.add(uuid)) {
            target?.sendMessage("[yellow]你已被禁言")
            returnReply("[green]已禁言{target}".with("target" to (target?.name ?: "目标")))
        } else {
            players.remove(uuid)
            target?.sendMessage("[green]你已被解除禁言")
            returnReply("[green]已解除{target}的禁言".with("target" to (target?.name ?: "目标")))
        }
    }
}

onEnable {
    val filter = Administration.ChatFilter { player, msg ->
        if (player.uuid() in players) {
            player.sendMessage("[yellow]你已被禁言")
            null
        } else msg
    }
    netServer.admins.chatFilters.add(filter)
    onDisable {
        netServer.admins.chatFilters.remove(filter)
    }
}

PermissionApi.registerDefault("wayzer.admin.mute", group = "@admin")