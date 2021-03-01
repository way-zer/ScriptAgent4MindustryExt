package coreMindustry.lib

import coreLibrary.lib.PermissionApi.*
import mindustry.gen.Player

object PermissionExt : HandlerWithFallback<Player>() {
    override fun handle(subject: Player, permission: String): Result {
        return Global.handle(subject.uuid(), permission)
    }
}

fun Player.hasPermission(permission: String) = PermissionExt.handle(this, permission).has