package wayzer.lib

import com.google.common.cache.CacheBuilder
import coreLibrary.lib.util.ServiceRegistry
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.net.Packets.ConnectPacket
import java.time.Duration

class PlayerData(val name: String, val uuid: String, val ids: Set<String> = mutableSetOf(uuid)) {
    var player: Player? = null
    var id: String = uuid
        private set
    val authed get() = id !== uuid

    fun addId(id: String, asPrimary: Boolean) {
        (ids as MutableSet).add(id)
        if (asPrimary) this.id = id
    }

    val shortId: String get() = IGetUidByShortId.getOrNull()?.getShortId(this) ?: id

    override fun toString(): String {
        return "PlayerData(id='$id', name='$name', authed=$authed)"
    }


    interface IGetUidByShortId {
        fun getShortId(data: PlayerData): String
        fun getUidByShortId(id: String): String?

        companion object : ServiceRegistry<IGetUidByShortId>()
    }

    companion object {
        val history = CacheBuilder.newBuilder()
            .expireAfterWrite(Duration.ofDays(1))
            .build<String, PlayerData>()!!
        private val preOnline = mutableMapOf<String, PlayerData>()
        private val online = mutableMapOf<Player, PlayerData>()

        fun forAuth(packet: ConnectPacket) = preOnline.getOrPut(packet.usid) {
            PlayerData(packet.name, packet.uuid)
        }

        operator fun get(player: Player): PlayerData = online.getOrPut(player) {
            if (player.con == null) error("player is not online")
            (preOnline.remove(player.usid()) ?: PlayerData(player.plainName(), player.uuid()))
                .also { it.player = player }
        }

        fun onLeave(player: Player) {
            val data = get(player)
            online.remove(player)
            history.put(player.uuid(), data)
            data.player = null
        }

        fun findByShortId(id: String): PlayerData? {
            val uuid = IGetUidByShortId.getOrNull()?.getUidByShortId(id) ?: id
            return Groups.player.find { it.uuid() == uuid }?.let { PlayerData[it] }
                ?: history.getIfPresent(uuid)
        }
    }
}