package wayzer

import arc.util.Time
import cf.wayzer.script_agent.listenTo
import coreLibrary.lib.event.PermissionRequestEvent
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.net.Packets
import java.util.*

name = "基础: 管理员与禁封"

var admins by config.key<Set<String>>(hashSetOf<String>(), "管理员列表(UUID)")
val pluginLog by config.key(dataDirectory.child("logs").child("secureLog.log").file()!!, "安全日记文件")

@Suppress("PropertyName")
val Admin = Admin()

inner class Admin : SharedData.IAdmin {
    override fun isAdmin(player: Player): Boolean {
        return player.isAdmin||admins.contains(player.uuid)
    }

    override fun ban(player: Player, uuid: String) {
        if (!isAdmin(player)) return
        val target = playerGroup.find { it.uuid == uuid } ?: return
        netServer.admins.banPlayerID(uuid)
        broadcast("[red] 管理员禁封了{target.name}".with("target" to target))
        secureLog("Ban", "${target.name} Ban ${target.name}(${target.uuid})")
    }

    override fun secureLog(tag: String, text: String) {
        ContentHelper.logToConsole("[red][$tag][yellow]$text")
        pluginLog.appendText("[$tag][${Date()}] $text\n")
    }
}
SharedData.admin = Admin

listen<EventType.PlayerBanEvent> {
    it.player?.info?.lastKicked = Time.millis()
    it.player?.con?.kick(Packets.KickReason.banned)
}

command("list", "列出当前玩家") {
    val list = playerGroup.all().map {
        "{player.name}[white]([red]{player.shortID}[white]) ".with("player" to it)
    }
    player!!.sendMessage("{list}".with("list" to list))
}
command("ban", "管理指令: 列出已ban用户，ban或解ban", {
    this.usage = "[3位id]"
    permission = "wayzer.admin.ban"
}) {
    val uuid = arg.getOrNull(0)
    if (uuid == null) {//list
        val sorted = netServer.admins.banned.sortedByDescending { it.lastKicked }
        val list = sorted.map {
            "[white]{info.name}[white]([red]{info.shortID}[] [white]{info.lastBan:MM/dd}[]),"
                    .with("info" to it)
        }
        player!!.sendMessage("Bans: {list}".with("list" to list))
    } else {
        netServer.admins.banned.find { it.id.startsWith(uuid) }?.let {
            netServer.admins.unbanPlayerID(it.id)
            Admin.secureLog("UnBan", "${player!!.name} unBan ${it.lastName}(${it.id})")
            return@command player!!.sendMessage("[green]解Ban成功 {info.name}".with("info" to it))
        }
        netServer.admins.getInfoOptional(uuid) ?: playerGroup.find { it.uuid.startsWith(uuid) }?.let {
            Admin.ban(player!!, it.uuid)
            return@command player!!.sendMessage("[green]Ban成功 {player.name}".with("player" to it))
        }
        player!!.sendMessage("[red]找不到改用户,请确定三位字母id输入正确! /list 或 /ban 查看".with())
    }
}
command("madmin", "列出或添加删除管理", {
    this.usage = "[uuid]"
    this.permission = "wayzer.admin.add"
}) {
    val uuid = arg.getOrNull(0)
    if (uuid == null) {
        val list = admins.map {
            "{info.name}({info.uuid},{info.lastJoin:MM/dd hh:mm}),".with("info" to netServer.admins.getInfo(it))
        }
        player!!.sendMessage("Admins: {list}".with("list" to list))
    } else {
        if (admins.contains(uuid)) {
            admins = admins - uuid
            return@command player!!.sendMessage("[red]$uuid [green] has been removed from Admins[]")
        }
        val info = netServer.admins.getInfoOptional(uuid)
                ?: return@command player!!.sendMessage("[red]Can't found player")
        admins = admins + uuid
        player!!.sendMessage("[red] ${info.lastName}($uuid) [green] has been added to Admins")
    }
}

listenTo<PermissionRequestEvent> {
    if (result != null) return@listenTo
    when (permission) {
        "wayzer.ext.observer", "wayzer.ext.history" -> result = true

        "wayzer.admin.ban", "wayzer.info.other",
        "wayzer.maps.host", "wayzer.maps.load", "wayzer.ext.team.change" -> if (context.player != null && admins.contains(context.player!!.uuid)) result = true

        "wayzer.admin.add", "wayzer.user.achieve",
        "main.spawnMob", "main.pixelPicture", "wayzer.user.genCode" -> {
        }
        else -> {
        }
    }
}