@file:Depends("coreLibrary/extApi/rpcService", "远程调用")

package wayzer.user

import java.io.Serializable
import java.rmi.Remote
import java.rmi.RemoteException
import java.text.DateFormat
import java.time.Duration
import java.time.Instant
import java.util.*

data class PlayerBan(
    val recordId: Int,
    val ids: Set<String>,
    val reason: String,
    val operator: String?,
    val createTime: Instant,
    val endTime: Instant
) : Serializable

interface PlayerBanStore : Remote {
    @Throws(RemoteException::class)
    fun findNotEnd(id: String): PlayerBan?
    @Throws(RemoteException::class)
    fun create(
        ids: Set<String>,
        duration: Duration,
        reason: String,
        operator: String?
    ): PlayerBan
    @Throws(RemoteException::class)
    fun delete(record: Int): PlayerBan?
}

val rpcService = contextScript<coreLibrary.extApi.RpcService>()
val store get() = rpcService.get<PlayerBanStore>()

fun Player.kick(ban: PlayerBan) {
    fun format(instant: Instant) = DateFormat.getDateTimeInstance().format(Date.from(instant))
    kick(
        """
        [red]你已在该服被禁封[]
        [yellow]名字: ${name()}
        [green]原因: ${ban.reason} (封禁ID#${ban.recordId})
        [green]禁封时间: ${format(ban.createTime)}
        [green]解禁时间: ${format(ban.endTime)}
        [yellow]如有问题,请截图此页咨询管理员
    """.trimIndent(), 0
    )
}

listen<EventType.PlayerConnect> {
    launch(Dispatchers.IO) {
        val ban = findBan(PlayerData[it.player]) ?: return@launch
        withContext(Dispatchers.game) {
            it.player.kick(ban)
        }
    }
}

suspend fun findBan(player: PlayerData): PlayerBan? = withContext(Dispatchers.IO) {
    player.ids.firstNotNullOfOrNull { id ->
        if (id == player.uuid && player.authed) return@firstNotNullOfOrNull null //skip uuid if authed
        store.findNotEnd(id)
    }
}

suspend fun ban(player: PlayerData, time: Int, reason: String, operate: Player?) {
    val ban = withContext(Dispatchers.IO) {
        store.create(
            player.ids,
            Duration.ofMinutes(time.toLong()), reason,
            operate?.let { PlayerData[it].id }
        )
    }
    Groups.player.filter {
        val info = PlayerData[it]
        info.ids.any { id ->
            if (id == info.uuid && info.authed) return@any false //skip uuid if authed
            id in player.ids
        }
    }.forEach {
        it.kick(ban)
        broadcast("[red] 管理员禁封了{target.name},原因: [yellow]{reason}".with("target" to it, "reason" to reason))
    }
}

command("banX", "管理指令: 禁封") {
    usage = "<3位id> <时间|分钟> <原因>"
    requirePermission("wayzer.admin.ban")
    body {
        if (arg.size < 3) replyUsage()
        val target = PlayerData.findByShortId(arg[0])
            ?: returnReply("[red]未找到目标, 请输入目标UUID/3位ID.".with())
        val time = arg[1].toIntOrNull()?.takeIf { it > 0 } ?: replyUsage()
        val reason = arg.slice(2 until arg.size).joinToString(" ")

        ban(target, time, reason, player)
        reply("[green]已禁封{qq}".with("qq" to (target)))
    }
}
command("unbanX", "管理指令: 解禁") {
    usage = "<id>"
    requirePermission("wayzer.admin.unban")
    body {
        if (arg.isEmpty()) replyUsage()
        val id = arg[0].toIntOrNull() ?: replyUsage()
        val ban = withContext(Dispatchers.IO) { store.delete(id) }
            ?: returnReply("[red]找不到封禁记录，检查ID是否正确".with())
        logger.info("unban ${ban.ids} ${ban.endTime} ${ban.reason}")
        reply("[green]解禁成功, 禁封原因: {reason}".with("reason" to ban.reason))
    }
}
PermissionApi.registerDefault("wayzer.admin.ban", "wayzer.admin.unban", group = "@admin")