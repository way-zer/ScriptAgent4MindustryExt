@file:Depends("wayzer/user/userService")

package wayzer.user.ext

import coreLibrary.DBApi
import mindustry.gen.Groups
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.user.UserService
import java.time.Duration

val userService = contextScript<UserService>()

onEnable {
    launch {
        DBApi.DB.awaitInit()
        while (true) {
            delay(5000)
            transaction {
                Groups.player
                    .mapNotNull { p -> PlayerData.findOrCreate(p).profile }
                    .filter { it.controlling }
                    .toSet().forEach { it.totalTime += 5 }
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
    val profile = PlayerData[it.player.uuid()].profile
    if (profile == null || finishProfile.contains(profile.id.value)) return@listen
    finishProfile.add(profile.id.value)
    userService.updateExp(profile, 3, "GoodGame")
}