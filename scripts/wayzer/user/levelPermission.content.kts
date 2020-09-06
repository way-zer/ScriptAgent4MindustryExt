package wayzer.user

import cf.wayzer.script_agent.listenTo
import coreLibrary.lib.event.PermissionRequestEvent

val PlayerProfile.level by PlaceHold.referenceForType<Int>("level")
listenTo<PermissionRequestEvent> {
    val profile = context.player?.let { PlayerData.getOrNull(it.uuid) }?.profile ?: return@listenTo
    result = when (permission) {
        "wayzer.user.skills.draug" -> profile.level >= 1
        "wayzer.user.skills.noFire" -> profile.level >= 2
        else -> return@listenTo
    }
}