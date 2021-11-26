package wayzer

import arc.util.Time
import mindustry.gen.Groups
import mindustry.net.Packets
import java.util.*

name = "基础: 禁封管理"

val pluginLog by config.key(Config.dataDirectory.resolve("secureLog.log"), "安全日记文件")

fun secureLog(tag: String, text: String) {
    ContentHelper.logToConsole("[red][$tag][yellow]$text")
    pluginLog.appendText("[$tag][${Date()}] $text\n")
}

/**
 * @param player null if is console
 */
fun ban(player: Player?, uuid: String) {
    val target = netServer.admins.getInfoOptional(uuid) ?: return
    netServer.admins.banPlayerID(uuid)
    broadcast("[red] 管理员禁封了{target.name}".with("target" to target))
    secureLog("Ban", "${player?.name ?: "Console"} Ban ${target.lastName}(${uuid})")
}
export(::secureLog, ::ban)

listen<EventType.PlayerBanEvent> {
    it.player?.info?.lastKicked = Time.millis()
    it.player?.con?.kick(Packets.KickReason.banned)
}

command("list", "列出当前玩家") {
    body {
        val list = Groups.player.map {
            "{player.name}[white]([red]{player.shortID}[white])".with("player" to it)
        }
        reply("{list}".with("list" to list))
    }
}
command("ban", "管理指令: 列出已ban用户，ban或解ban") {
    usage = "[3位id]"
    permission = "wayzer.admin.ban"
    body {
        val uuid = arg.getOrNull(0)
        if (uuid == null || uuid.length < 3) {//list
            val sorted = netServer.admins.banned.sortedByDescending { it.lastKicked }
            val list = sorted.map {
                "[white]{info.name}[white]([red]{info.shortID}[] [white]{info.lastBan:MM/dd}[])"
                    .with("info" to it)
            }
            reply("Bans: {list}".with("list" to list))
        } else {
            netServer.admins.banned.find { it.id.startsWith(uuid) }?.let {
                netServer.admins.unbanPlayerID(it.id)
                secureLog("UnBan", "${player!!.name} unBan ${it.lastName}(${it.id})")
                returnReply("[green]解Ban成功 {info.name}".with("info" to it))
            }
            returnReply("[red]封禁请使用banX指令".with())
//            val target = netServer.admins.getInfoOptional(uuid)?.id
//                ?: getUUIDbyShort(uuid)
//                ?: returnReply("[red]找不到该用户,请确定三位字母id输入正确! /list 或 /ban 查看".with())
//            ban(player, target)
        }
    }
}
command("madmin", "列出或添加删除管理") {
    this.usage = "[uuid/qq]"
    this.permission = "wayzer.permission.admin"
    body {
        val uuid = arg.getOrNull(0)
        if (uuid == null) {
            val list = PermissionApi.allKnownGroup.filter { PermissionApi.handleGroup(it, "@admin").has }
            returnReply("Admins: {list:,}".with("list" to list))
        } else {
            reply("[yellow]建议使用/sa permission管理用户与@admin组".with())
            val isQQ = uuid.length > 5 && uuid.toLongOrNull() != null
            val key = if (isQQ) "qq$uuid" else uuid
            if (PermissionApi.handleGroup(key, "@admin").has) {
                RootCommands.handleInput("sa permission $key remove @admin", null)
                returnReply("[red]{uuid} [green] has been removed from Admins[]".with("uuid" to uuid))
            } else {
                if (isQQ) {
                    RootCommands.handleInput("sa permission $key add @admin", null)
                    reply("[green]QQ [red]{qq}[] has been added to Admins".with("qq" to uuid))
                } else {
                    val info = netServer.admins.getInfoOptional(uuid)
                        ?: returnReply("[red]Can't found player".with())
                    RootCommands.handleInput("sa permission $key add @admin", null)
                    reply("[green]Player [red]{info.name}({info.uuid})[] has been added to Admins".with("info" to info))
                }
            }
        }
    }
}

PermissionApi.registerDefault("wayzer.admin.*", group = "@admin")