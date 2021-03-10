package coreMindustry.lib

import coreLibrary.lib.PermissionApi
import mindustry.gen.Player

fun Player.hasPermission(permission: String): Boolean {
    return PermissionApi.handleThoughEvent(this, permission, listOf(uuid())).has
}