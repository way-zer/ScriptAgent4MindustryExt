package wayzer.lib

import cf.wayzer.placehold.PlaceHoldApi
import cf.wayzer.scriptAgent.getContextScript
import coreLibrary.lib.PermissionApi.*
import coreMindustry.lib.PermissionExt as PlayerPermission
import mindustry.gen.Player
import wayzer.lib.dao.PlayerData
import wayzer.lib.dao.PlayerProfile

/**
 * 已经扩展Player,无需直接调用该接口
 * 除非想通过PlayerData 或者 PlayerProfile获取权限
 * @see PermissionExt
 */
object PermissionExt : HandlerWithFallback<PlayerProfile>() {
    fun handle(subject: PlayerData, permission: String): Result {
        return subject.profile?.let { handle(it, permission) } ?: Result.Default
    }

    override fun handle(subject: PlayerProfile, permission: String): Result {
        var result = Global.handle("qq${subject.qq}", permission)
        val level = (PlaceHoldApi.GlobalContext.typeResolve(subject, "level") ?: -1) as Int
        for (lvl in level downTo 0) {
            result = result.fallback { Global.handle("@lvl$lvl", permission) }
        }
        return result
    }

    object ExtPlayer : HandlerWithFallback<Player>() {
        override fun handle(subject: Player, permission: String): Result {
            return handle(PlayerData[subject.uuid()], permission)
        }
    }

    init {
        javaClass.getContextScript().apply {
            onEnable {
                ExtPlayer.fallback = PlayerPermission.fallback
                PlayerPermission.fallback = ExtPlayer
            }
            onDisable {
                PlayerPermission.fallback = ExtPlayer.fallback
            }
        }
    }
}