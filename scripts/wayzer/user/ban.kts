package wayzer.user

import coreLibrary.DBApi.DB.registerTable
import mindustry.net.Packets
import org.jetbrains.exposed.sql.transactions.transaction
import java.text.DateFormat
import java.time.Duration
import java.time.Instant
import java.util.*

registerTable(PlayerBan.T)

fun Player.kick(profile: PlayerProfile, ban: PlayerBan) {
    fun format(instant: Instant) = DateFormat.getDateTimeInstance().format(Date.from(instant))
    kick(
        """
        [red]你已在该服被禁封[]
        [yellow]名字: ${name()} [yellow]绑定qq: ${profile.qq}
        [green]原因: ${ban.reason}
        [green]禁封时间: ${format(ban.createTime)}
        [green]解禁时间: ${format(ban.endTime)}
        [yellow]如有问题,请截图此页咨询管理员
    """.trimIndent(), 0
    )
}

listen<EventType.PlayerConnect> {
    val profile = PlayerData.findById(it.player.uuid())?.profile ?: return@listen
    launch(Dispatchers.IO) {
        val ban = transaction { PlayerBan.findNotEnd(profile.id) } ?: return@launch
        withContext(Dispatchers.game) {
            it.player.kick(profile, ban)
        }
    }
}

suspend fun ban(uuid: String, time: Int, reason: String, operate: PlayerProfile?): PlayerProfile? {
    val profile = withContext(Dispatchers.IO) {
        transaction { PlayerData.findByIdWithTransaction(uuid) }?.profile
    }
    if (profile == null) {
        netServer.admins.banPlayerID(uuid)
        Groups.player.filter { it.uuid() == uuid }.forEach { it.kick(Packets.KickReason.banned) }
        netServer.admins.getInfoOptional(uuid)?.let {
            broadcast("[red] 管理员禁封了{target.name},原因: [yellow]{reason}".with("target" to it, "reason" to reason))
        }
    } else {
        val ban = withContext(Dispatchers.IO) {
            transaction {
                PlayerBan.create(profile, Duration.ofMinutes(time.toLong()), reason, operate)
            }
        }
        profile.players.toTypedArray().forEach {
            it.kick(profile, ban)
            broadcast("[red] 管理员禁封了{target.name},原因: [yellow]{reason}".with("target" to it, "reason" to reason))
        }
    }
    return profile
}

export(::ban)

command("banX", "管理指令: 禁封") {
    usage = "<3位id> <时间|分钟> <原因>"
    permission = "wayzer.admin.ban"
    body {
        if (arg.size < 3) replyUsage()
        val uuid = netServer.admins.getInfoOptional(arg[0])?.id
            ?: depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(arg[0])
            ?: returnReply("[red]请输入目标3位ID,不清楚可通过/list查询".with())
        val time = arg[1].toIntOrNull()?.takeIf { it > 0 } ?: replyUsage()
        val reason = arg.slice(2 until arg.size).joinToString(" ")
        val operate = player?.let { PlayerData[it.uuid()].profile }
        val qq = ban(uuid, time, reason, operate)?.qq
        reply("[green]已禁封{qq}".with("qq" to (qq ?: uuid)))
    }
}
PermissionApi.registerDefault("wayzer.admin.ban", group = "@admin")