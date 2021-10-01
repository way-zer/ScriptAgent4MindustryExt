package wayzer.user

import coreLibrary.DBApi
import mindustry.gen.Groups
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

onEnable {
    launch {
        DBApi.DB.awaitInit()
        val onlinePlayer = Groups.player.mapNotNull { PlayerData[it.uuid()].profile?.id }
        val num = transaction {
            PlayerProfile.T.update({
                (PlayerProfile.T.online eq Setting.serverId) and (PlayerProfile.T.id notInList onlinePlayer)
            }) {
                it[online] = null
            }
        }
        logger.info("清理残留记录: $num")
    }
}