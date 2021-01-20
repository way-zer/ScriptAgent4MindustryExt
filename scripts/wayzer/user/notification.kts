@file:Import("@wayzer/services/UserService.kt", sourceFile = true)
package wayzer.user

import mindustry.gen.Groups
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.services.UserService
import java.time.Instant

name = "通知服务"
val userService by ServiceRegistry<UserService>()

fun notify(profile: PlayerProfile, message: String, params: Map<String, String>, broadcast: Boolean = false) {
    transaction {
        PlayerNotification.new(profile, message, params, broadcast)
    }
}
export(::notify)

val map = mutableMapOf<Int, Instant>()//profile -> lastTime
listen<EventType.PlayerLeave> {
    map.remove(PlayerData[it.player.uuid()].profile?.id?.value)
}

fun List<PlayerNotification>.run(profile: PlayerProfile, players: List<Player>) {
    launch(Dispatchers.game) {
        forEach {
            if (it.broadcast)
                broadcast(
                    it.message.with(
                        *it.params.map { e -> e.key to e.value }.toTypedArray(),
                        "player" to players.first()
                    )
                )
            else players.forEach { p ->
                p.sendMessage(it.message.with(*it.params.map { e -> e.key to e.value }.toTypedArray(), "player" to p))
            }
            if ("dotExp" in it.params) userService.updateExp(profile, 0)//updateIcon
        }
    }
}

fun loop() {
    Groups.player.groupBy { PlayerData[it.uuid()].profile }.forEach { (profile, players) ->
        if (profile == null) return@forEach
        val lastTime = map.getOrPut(profile.id.value) { profile.lastTime }
        transaction {
            val value = PlayerNotification.getNew(profile, lastTime).toList()
            if (value.isEmpty()) return@transaction null
            profile.refresh()
            if (profile.controlling) profile.lastTime = Instant.now()
            return@transaction value
        }?.run(profile, players)
        map[profile.id.value] = Instant.now()
    }
}

onEnable {
    launch {
        while (enabled) {
            delay(5000)
            loop()
        }
    }
}