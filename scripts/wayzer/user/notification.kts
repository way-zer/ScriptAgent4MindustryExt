package wayzer.user

import coreLibrary.DBApi
import coreLibrary.DBApi.DB.registerTable
import org.jetbrains.exposed.sql.transactions.transaction

name = "通知服务"

/** Should call in [Dispatchers.IO] */
fun notify(profile: PlayerProfile, message: String, params: Map<String, String>, broadcast: Boolean = false) {
    transaction {
        NotificationEntity.new(profile, message, params, broadcast)
    }
}
export(::notify)


fun List<NotificationEntity>.run(profile: PlayerProfile) {
    val players = profile.players
    launch(Dispatchers.game) {
        if (players.isEmpty()) return@launch
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
    PlayerProfile.allOnline.toList().forEach { profile ->
        transaction {
            NotificationEntity.getNew(profile).toList()
        }.takeUnless { it.isEmpty() }?.run(profile)
    }
}

registerTable(NotificationEntity.T, NotificationEntity.TimeTable)
onEnable {
    launch {
        DBApi.DB.awaitInit()
        while (enabled) {
            delay(5000)
            loop()
        }
    }
}