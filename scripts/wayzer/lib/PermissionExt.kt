package wayzer.lib

import cf.wayzer.scriptAgent.getContextScript
import cf.wayzer.scriptAgent.listenTo
import coreLibrary.lib.PermissionApi
import coreLibrary.lib.PermissionApi.*
import coreLibrary.lib.event.RequestPermissionEvent
import mindustry.gen.Player
import wayzer.lib.dao.PlayerData
import wayzer.lib.dao.PlayerProfile

fun PlayerProfile.hasPermission(permission: String): Boolean {
    return PermissionApi.handleThoughEvent(this, permission, listOf("qq$qq")).has
}

fun PlayerData.hasPermission(permission: String, skipSecure: Boolean): Boolean {
    if (!skipSecure) error("This Api can not be secure")
    return profile?.hasPermission(permission) ?: false
}

object PermissionExt {
    init {
        javaClass.getContextScript().apply {
            listenTo<RequestPermissionEvent> {
                val player = this.subject as? Player ?: return@listenTo
                val profile = PlayerData[player.uuid()].secureProfile(player) ?: return@listenTo
                val newGroup = group.toMutableList()
                newGroup.add(1, "qq${profile.qq}")
                group = newGroup
            }
        }
    }
}