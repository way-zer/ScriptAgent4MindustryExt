package wayzer.user

import cf.wayzer.script_agent.listenTo
import coreLibrary.lib.event.PermissionRequestEvent

val PlayerProfile.level by PlaceHold.referenceForType<Int>("level")
listenTo<PermissionRequestEvent> {
    val profile = context.player?.let { PlayerData.getOrNull(it.uuid) }?.profile ?: return@listenTo
    when (permission) {
        "wayzer.user.skills.draug" -> profile.level >= 2
    }
}
state