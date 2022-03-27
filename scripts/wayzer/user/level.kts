@file:Depends("wayzer/user/userService")

package wayzer.user

import cf.wayzer.placehold.DynamicVar
import coreLibrary.lib.event.RequestPermissionEvent
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

val showIcon by config.key(true, "是否显示等级图标")
val userService = contextScript<UserService>()

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

fun updateExp(p: PlayerProfile, desc: String, dot: Int) {
    if (dot != 0) {
        userService.notify(
            p, "[green]经验+{dotExp}{desc}", mapOf(
                "dotExp" to dot.toString(), "desc" to if (desc.isEmpty()) "" else "([cyan]$desc[])"
            )
        )
        transaction {
            p.refresh()
            p.totalExp += dot
        }
        if (level(p.totalExp) != level(p.totalExp - dot)) {
            userService.notify(p, "[gold]恭喜你成功升级到{level}级", mapOf("level" to level(p.totalExp).toString()))
        }
    }
}
export(::updateExp)

registerVarForType<Player>().apply {
    registerChild("prefix.0lvl", "等级图标显示", DynamicVar.obj {
        if (!showIcon) return@obj ""
        "<${getIcon(level(PlayerData[it.uuid()].secureProfile(it)?.totalExp ?: 0))}>"
    })
}

listenTo<RequestPermissionEvent> {
    val profile = when (val p = subject) {
        is PlayerProfile -> p
        is Player -> PlayerData[p.uuid()].secureProfile(p) ?: return@listenTo
        else -> return@listenTo
    }
    val index = group.indexOfLast { !it.startsWith("@") }
    val newGroup = group.toMutableList()
    newGroup.addAll(index + 1, (level(profile.totalExp) downTo 0).map { "@lvl$it" })
    group = newGroup
}