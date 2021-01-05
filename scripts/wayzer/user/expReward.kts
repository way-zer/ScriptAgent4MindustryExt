package wayzer.user

import mindustry.entities.type.Player
import mindustry.game.EventType
import java.time.Duration

onEnable {
    launch {
        while (true) {
            delay(5000)
            playerGroup.mapNotNull { PlayerData.getOrNull(it.uuid)?.profile }.toSet().forEach {
                it.totalTime += 5
            }
        }
    }
}

var endTime = false
val finishProfile = mutableSetOf<Int>()
listen<EventType.GameOverEvent> {
    val gameTime by PlaceHold.reference<Duration>("state.gameTime")
    if (gameTime > Duration.ofMinutes(20)) {
        endTime = true
    }
}
listen<EventType.ResetEvent> {
    endTime = false
    finishProfile.clear()
}
listen<EventType.PlayerChatEvent> {
    if (!endTime || !it.message.equals("gg", true)) return@listen
    val updateExp = depends("wayzer/user/level")?.import<PlayerProfile.(Int) -> List<Player>>("updateExp")
    if (updateExp != null) {
        val profile = PlayerData[it.player.uuid].profile
        if (profile == null || finishProfile.contains(profile.id.value)) return@listen
        finishProfile.add(profile.id.value)
        profile.updateExp(3).forEach { p ->
            p.sendMessage("[green]经验 +3")
        }
    }
}