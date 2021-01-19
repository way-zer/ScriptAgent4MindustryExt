package wayzer.user

import mindustry.gen.Groups
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant

name = "通知服务"

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

fun List<PlayerNotification>.run(players: List<Player>) {
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
            if (profile.controlling) profile.lastTime = Instant.now()
            return@transaction value
        }?.run(players)
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