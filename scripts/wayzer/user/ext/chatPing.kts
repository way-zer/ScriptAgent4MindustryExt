@file:Depends("wayzer/user/shortID")

package wayzer.user.ext

import arc.util.Strings
import mindustry.net.Administration

val regex = Regex("@([a-zA-Z0-9+/]{3})")

val filter = Administration.ChatFilter { p, msg ->
    val players = mutableListOf<Player>()
    msg.replace(regex) { match ->
        val id = match.groupValues[1]
        //TODO no runBlocking
        val name = if (id == "all" && runBlocking { p.hasPermission("$dotId.pingAll") }) {
            players.addAll(Groups.player)
            "全体成员"
        } else {
            val player = PlayerData.findByShortId(id)?.player ?: return@replace match.value
            players.add(player)
            Strings.stripColors(player.name)
        }
        " [gold]@$name[] "
    }.also { newMsg ->
        players.forEach {
            Call.announce(it.con, "[red]有人在聊天区@你，请注意查看:[]\n$newMsg")
        }
    }
}
onEnable { netServer.admins.chatFilters.add(filter) }
onDisable { netServer.admins.chatFilters.remove(filter) }