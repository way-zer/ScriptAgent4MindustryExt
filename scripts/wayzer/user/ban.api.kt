package wayzer.user

import mindustry.gen.Player
import wayzer.lib.PlayerData

interface BanService {
    suspend fun findBan(player: PlayerData): PlayerBan?
    suspend fun ban(player: PlayerData, time: Int, reason: String, operate: Player?)
}