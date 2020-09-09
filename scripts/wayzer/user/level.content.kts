package wayzer.user

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.entities.type.Player
import mindustry.game.EventType
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

val customWelcome by config.key(false, "是否开启自定义进服信息(中文)")

fun getIcon(level: Int): Char {
    if (level <= 0) return (63611).toChar()
    return (63663 - min(level, 12)).toChar()
    //0级为电池,1-12级为铜墙到合金墙
}

fun level(exp: Int) = floor(sqrt(max(exp, 0).toDouble()) / 10).toInt()
fun expByLevel(level: Int) = level * level * 100

registerVarForType<PlayerProfile>().apply {
    registerChild("totalExp", "总经验", DynamicVar { obj, _ -> obj.totalExp })
    registerChild("level", "当前等级", DynamicVar { obj, _ -> level(obj.totalExp) })
    registerChild("levelIcon", "当前等级图标", DynamicVar { obj, _ -> getIcon(level(obj.totalExp)) })
    registerChild("nextLevel", "下一级的要求经验值", DynamicVar { obj, _ -> expByLevel(level(obj.totalExp) + 1) })
}

/**
 * @return 所有在线用户
 */
fun updateExp(p: PlayerProfile, dot: Int): List<Player> {
    val players = playerGroup.filter { PlayerData.getOrNull(it.uuid ?: "")?.profile == p }
    p.apply {
        totalExp += dot
        if (level(totalExp) != level(totalExp - dot)) {
            players.forEach {
                it.sendMessage("[gold]恭喜你成功升级到{level}级".with("level" to level(totalExp)))
                it.name = it.name.replace(Regex("<.>"), "<${getIcon(level(totalExp))}>")
            }
        }
    }
    return players
}
export(::updateExp)

listen<EventType.PlayerConnect> {
    Core.app.post {
        it.player.apply {
            PlayerData.getOrNull(uuid)?.profile?.let { profile ->
                name = "[white]<${getIcon(level(profile.totalExp))}>[#$color]$name"
            }
        }
    }
}

listen<EventType.PlayerJoin> {
    if (!customWelcome) return@listen
    it.player.sendMessage("[cyan][+]{player.name} [gold]加入了服务器".with("player" to it.player))
    broadcast("[cyan][+]{player.name} [goldenrod]加入了服务器".with("player" to it.player))
}
