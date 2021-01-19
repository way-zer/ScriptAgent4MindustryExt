@file:Import("@wayzer/services/UserService.kt", sourceFile = true)

package wayzer.user

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi.with
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.services.UserService
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

val customWelcome by config.key(false, "是否开启自定义进服信息(中文)")
val userService by ServiceRegistry<UserService>()

fun getIcon(level: Int): Char {
    if (level <= 0) return (63611).toChar()
    return (63663 - min(level, 12)).toChar()
    //0级为电池,1-12级为铜墙到合金墙
}

fun level(exp: Int) = floor(sqrt(max(exp, 0).toDouble()) / 10).toInt()
fun expByLevel(level: Int) = level * level * 100

registerVarForType<PlayerProfile>().apply {
    registerChild("level", "当前等级", DynamicVar.obj { level(it.totalExp) })
    registerChild("levelIcon", "当前等级图标", DynamicVar.obj { getIcon(level(it.totalExp)) })
    registerChild("nextLevel", "下一级的要求经验值", DynamicVar.obj { expByLevel(level(it.totalExp) + 1) })
}

/**
 * @return 所有在线用户
 */
fun updateExp(p: PlayerProfile, dot: Int) {
    userService.notify(p, "[green]经验+{dot}", mapOf("dot" to dot.toString()))
    transaction {
        p.refresh()
        p.totalExp += dot
    }
    if (level(p.totalExp) != level(p.totalExp - dot)) {
        userService.notify(p, "[gold]恭喜你成功升级到{level}级", mapOf("level" to level(p.totalExp).toString()))
    }
}
export(::updateExp)

listen<EventType.PlayerConnect> {
    Core.app.post {
        it.player.apply {
            name = "[white]<${getIcon(level(PlayerData.findById(uuid())?.profile?.totalExp ?: 0))}>[#$color]$name"
        }
    }
}

listen<EventType.PlayerJoin> {
    if (!customWelcome) return@listen
    it.player.sendMessage("[cyan][+]{player.name} [gold]加入了服务器".with("player" to it.player))
    broadcast("[cyan][+]{player.name} [goldenrod]加入了服务器".with("player" to it.player))
}
