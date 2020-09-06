package wayzer.user

import mindustry.entities.type.Player
import mindustry.game.EventType
import java.time.Duration
import java.time.Instant
import java.util.*

onEnable {
    launch {
        while (true) {
            delay(5000)
            playerGroup.mapNotNull { PlayerData[it.uuid].profile }.toSet().forEach {
                it.totalTime += 5
            }
        }
    }
}

var endTime = false
val finishProfile = mutableSetOf<Int>()
listen<EventType.GameOverEvent> {
    val startTime by PlaceHold.reference<Date>("state.startTime")
    if (Duration.between(startTime.toInstant(), Instant.now()) > Duration.ofMinutes(20)) {
        endTime = true
    }
}
listen<EventType.ResetEvent> {
    endTime = false
    finishProfile.clear()
}
listen<EventType.PlayerChatEvent> {
    if (!endTime || !it.message.equals("gg", true)) return@listen
    val updateExp = depends("wayzer/user/level")?.import<Player.(Int) -> Boolean>("updateExp")
    if (updateExp != null) {
        val id = PlayerData[it.player.uuid].profile?.id?.value
        if (id == null || finishProfile.contains(id)) return@listen
        if (it.player.updateExp(3)) {
            it.player.sendMessage("[green]经验 +3")
            finishProfile.add(id)
        }
    }
}