@file:Depends("coreLibrary/DBApi", "数据库储存")

package wayzer.user

import coreLibrary.DBApi.DB.registerTable
import java.text.DateFormat
import java.time.Duration
import java.time.Instant
import java.util.*

registerTable(PlayerBan.T)

fun Player.kick(ban: PlayerBan) {
    fun format(instant: Instant) = DateFormat.getDateTimeInstance().format(Date.from(instant))
    kick(
        """
        [red]你已在该服被禁封[]
        [yellow]名字: ${name()}
        [green]原因: ${ban.reason} (封禁ID#${ban.id})
        [green]禁封时间: ${format(ban.createTime)}
        [green]解禁时间: ${format(ban.endTime)}
        [yellow]如有问题,请截图此页咨询管理员
    """.trimIndent(), 0
    )
}

listen<EventType.PlayerConnect> {
    launch(Dispatchers.IO) {
        val ban = PlayerBan.findNotEnd(PlayerData[it.player].id) ?: return@launch
        withContext(Dispatchers.game) {
            it.player.kick(ban)
        }
    }
}

suspend fun ban(player: PlayerData, time: Int, reason: String, operate: Player?) {
    val ban = withContext(Dispatchers.IO) {
        PlayerBan.create(
            player, Duration.ofMinutes(time.toLong()), reason,
            operate?.let { PlayerData[it].id }
        )
    }
    Groups.player.filter { PlayerData[it].id in player.ids }.forEach {
        it.kick(ban)
        broadcast("[red] 管理员禁封了{target.name},原因: [yellow]{reason}".with("target" to it, "reason" to reason))
    }
}

command("banX", "管理指令: 禁封") {
    usage = "<3位id> <时间|分钟> <原因>"
    permission = "wayzer.admin.ban"
    body {
        if (arg.size < 3) replyUsage()
        val uuid = netServer.admins.getInfoOptional(arg[0])?.id
            ?: depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(arg[0])
            ?: returnReply("[red]请输入目标3位ID,不清楚可通过/list查询".with())
        val snapshot = Groups.player.find { it.uuid() == uuid }?.let { PlayerData[it] }
            ?: PlayerData.history.getIfPresent(uuid) ?: returnReply("[red]未找到目标".with())
        val time = arg[1].toIntOrNull()?.takeIf { it > 0 } ?: replyUsage()
        val reason = arg.slice(2 until arg.size).joinToString(" ")

        ban(snapshot, time, reason, player)
        reply("[green]已禁封{qq}".with("qq" to (uuid)))
    }
}
command("unbanX", "管理指令: 解禁") {
    usage = "<id>"
    permission = "wayzer.admin.unban"
    body {
        if (arg.isEmpty()) replyUsage()
        val id = arg[0].toIntOrNull() ?: replyUsage()
        val ban = PlayerBan.delete(id) ?: returnReply("[red]找不到封禁记录，检查ID是否正确".with())
        logger.info("unban ${ban.ids} ${ban.endTime} ${ban.reason}")
        reply("[green]解禁成功, 禁封原因: {reason}".with("reason" to ban.reason))
    }
}
PermissionApi.registerDefault("wayzer.admin.ban", "wayzer.admin.unban", group = "@admin")