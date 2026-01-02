@file:Depends("wayzer/map/betterTeam", "强制观察者")
@file:Depends("coreMindustry/menu", "菜单")
@file:Depends("coreLibrary/extApi/KVStore", "储存记录")
@file:Depends("coreLibrary/extApi/rpcService", "RPC通讯实现Cache持久化和ip快速登录")

package wayzer.user.ext

import arc.util.Strings
import arc.util.serialization.Jval
import coreMindustry.MenuV2
import mindustry.net.Packets
import org.h2.mvstore.type.StringDataType
import wayzer.map.BetterTeam
import java.net.URL
import java.net.URLEncoder
import java.rmi.Remote
import java.rmi.RemoteException
import java.rmi.server.UnicastRemoteObject
import java.util.*
import kotlin.time.Duration.Companion.minutes

interface AuthCache : Remote {
    @Throws(RemoteException::class)
    fun get(uid: String, usid: String, ip: String): String?
    @Throws(RemoteException::class)
    fun put(uid: String, usid: String, ip: String, gid: String)
}

class CacheImpl(private val map: MutableMap<String, String>) : UnicastRemoteObject(), AuthCache {
    override fun get(uid: String, usid: String, ip: String): String? {
        val gid = map["$uid/$usid"] ?: map["IP/$uid/$ip"]
        if (gid != null) put(uid, usid, ip, gid)
        return gid
    }

    override fun put(uid: String, usid: String, ip: String, gid: String) {
        map["$uid/$usid"] = gid
        map["IP/$uid/$ip"] = gid
    }
}

class WithCache(private val cache: AuthCache, private val fallback: AuthCache) : AuthCache {
    override fun get(uid: String, usid: String, ip: String): String? {
        cache.get(uid, usid, ip)?.let { return it }
        return fallback.get(uid, usid, ip)?.also { cache.put(uid, usid, ip, it) }
    }

    override fun put(uid: String, usid: String, ip: String, gid: String) {
        cache.put(uid, usid, ip, gid)
        fallback.put(uid, usid, ip, gid)
    }
}

@Savable(false)
var serverId = UUID.randomUUID().toString()
val rpcService = contextScript<coreLibrary.extApi.RpcService>()
val localCache = CacheImpl(
    contextScript<coreLibrary.extApi.KVStore>().open("authCache", StringDataType.INSTANCE)
)
val store: AuthCache
    get() {
        var store = rpcService.get<AuthCache>()
        if (!rpcService.isMaster) store = WithCache(localCache, store)
        return store
    }
onEnable {
    rpcService.register<AuthCache> { localCache }
}


data class AuthRes(val gid: String?, val authUrl: String)

suspend fun auth(uid: String, usid: String, ip: String, name: String): AuthRes = withContext(Dispatchers.IO) {
    store.get(uid, usid, ip)?.let { return@withContext AuthRes(it, "") }

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
                store.put(uid, usid, ip, it.gid)
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
val teams = contextScript<BetterTeam>()

val forceAuth
    get() = mode == Mode.Force ||
            (mode == Mode.Auto && Groups.player.size() >= forceWhenPlayers)
listenTo<BetterTeam.AssignTeamEvent>(Event.Priority.Intercept) {
    if (PlayerData[player].authed) return@listenTo
    if (mode == Mode.Silent) return@listenTo
    if (forceAuth)
        team = teams.spectateTeam
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
        val player = context.player ?: return@body
        openMenu(player)
    }
}