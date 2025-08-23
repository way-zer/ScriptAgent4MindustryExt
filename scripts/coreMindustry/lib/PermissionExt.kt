package coreMindustry.lib

import coreLibrary.lib.PermissionApi
import mindustry.gen.Player

suspend fun Player.hasPermission(permission: String): Boolean {
    val groups = buildList {
        add(uuid())
        if (admin) add("@admin")
    }
    return PermissionApi.handleThoughEvent(this, permission, groups).has
}