package wayzer.pvp

import arc.util.Log
import mindustry.game.Team
import mindustry.net.Administration


fun message(from: Player, msg: String, teamMsg: Boolean) {
    for (p in Groups.player) {
        val prefix = when {
            !teamMsg && from.team().id == 255 -> "[cyan][公屏][观战]"
            !teamMsg -> "[cyan][公屏][[${from.team().coloredName()}]"
            from.team() == p.team() && from.team().id == 255 -> "[violet][观战]"
            from.team() == p.team() -> "[violet][队内]"
            p.team() == Team.all[255] -> "[violet][[${from.team().coloredName()}队内]"
            else -> continue
        }
        p.sendMessage(prefix + netServer.chatFormatter.format(from, msg), from, msg)
    }
}

val filter = Administration.ChatFilter { p, t ->
    if (!state.rules.pvp) return@ChatFilter t
    message(p, t, true)
    Log.info("&fi@: @", "&lc" + p.name, "&lw$t")
    null
}

onEnable {
    netServer.admins.chatFilters.add(filter)
    onDisable {
        netServer.admins.chatFilters.remove(filter)
    }
}

command("t", "PVP模式全体聊天") {
    type = CommandType.Client
    body {
        val msg = arg.joinToString(" ")
        message(player!!, msg, false)
    }
}