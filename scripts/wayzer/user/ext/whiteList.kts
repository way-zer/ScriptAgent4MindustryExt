@file:Depends("wayzer/map/betterTeam", "强制观察者")
@file:Depends("coreMindustry/menu", "菜单")

package wayzer.user.ext

import arc.util.Strings
import arc.util.serialization.Jval
import coreMindustry.MenuV2
import mindustry.net.Packets
import wayzer.map.AssignTeamEvent
import java.net.URL
import java.net.URLEncoder
import java.util.*
import kotlin.time.Duration.Companion.minutes


@Savable(false)
var serverId = UUID.randomUUID().toString()
val store: AuthCache? by Services.get<AuthCache>().nullable

data class AuthRes(val gid: String?, val authUrl: String)

suspend fun auth(uid: String, usid: String, ip: String, name: String): AuthRes = withContext(Dispatchers.IO) {
    store?.get(uid, usid, ip)?.let { return@withContext AuthRes(it, "") }

    val nameE = URLEncoder.encode(Strings.stripColors(name), Charsets.UTF_8)
    val url = "https://api.mindustry.top/servers/auth/check" +
            "?uid=${uid.replace("+", "%2B")}&state=$serverId&clientIp=${ip}&name=${nameE}"
    val res = runInterruptible { URL(url).readText() }
    Jval.read(res).run {
        AuthRes(
            get("gid")?.takeIf { it.isString }?.asString(),
            getString("authUrl")
        ).also {
            if (it.gid != null) {
                store?.put(uid, usid, ip, it.gid)
            }
        }
    }
}

listenTo<ConnectAsyncEvent> {
    val data = PlayerData.forAuth(packet)
    if (data.authed) return@listenTo
    val res = auth(packet.uuid, packet.usid, con.address, packet.name)
    res.gid?.let { data.addId(it, asPrimary = true) }
}

registerVarForType<Player>().apply {
    registerChild("prefix.1-notAuth", "游客标识") {
        if (!PlayerData[it].authed) "[green][游客][]" else null
    }
}

fun openMenu(player: Player) {
    MenuV2(player, followup = true) {
        msg = """
        欢迎来到[gold]微泽[]服务器
        使用[pink]统一登录[]才进行游戏

        请点击下方登录按钮，在浏览器完成登录，返回后再次点击即可完成登录。
        如有问题，可添加QQ群[acid]1044335057[]
        """.trimIndent().with().toPlayer(player)
        option("资源站统一登录") {
            val res = auth(player.uuid(), player.usid(), player.con.address, player.name)
            res.gid?.let {
                //rejoin, better with ban plugin
                player.lastText = "[Silent_Leave]"
                player.kick(Packets.KickReason.serverRestarting)
                close()
                return@option
            }
            Call.openURI(player.con(), res.authUrl)
        }
        column(2) {
            option("QQ交流群") {
                Call.openURI(
                    player.con,
                    "TY3vTneqEjtixqEPariJ2uAQ67jiTxvA&jump_from=webapi&authKey=Rs21q/PsyDIFrtW3sgDfxhXuV1gaHcc7k/JwKkAWi/NuIKCDh75FD7uee0E/yGFw"
                )
            }
            option("Discord") {
                Call.openURI(player.con, "https://discord.gg/XVkjgZ8nPM")
            }
        }
        if (mode != Mode.Force)
            option("下次再说") {
                close()
            }
    }.let {
        launch(Dispatchers.game) { it.send().awaitWithTimeout(10.minutes) }
    }
}

enum class Mode {
    Silent, Menu, Force, Auto
}

val mode by config.key(Mode.Silent, "统一登录模式")
val forceWhenPlayers by config.key(8, "人多时自动启动白名单，需要mode=Auto时生效")

val forceAuth
    get() = mode == Mode.Force ||
            (mode == Mode.Auto && Groups.player.size() >= forceWhenPlayers)
listenTo<AssignTeamEvent>(Event.Priority.Intercept) {
    if (PlayerData[player].authed) return@listenTo
    if (mode == Mode.Silent) return@listenTo
    if (forceAuth)
        team = AssignTeamEvent.spectateTeam
    openMenu(player)
}

listenTo<RequestPermissionEvent> {
    val player = this.subject as? Player ?: return@listenTo
    if (PlayerData[player].authed) {
        group += "@authed"
    } else {
        if (forceAuth) directReturn(PermissionApi.Result.Reject)
    }
}

command("login", "统一登录") {
    attr(ClientOnly)
    body {
        val player = player
            ?: return@body
        openMenu(player)
    }
}