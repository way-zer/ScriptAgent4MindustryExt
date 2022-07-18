package wayzer.lib.event

import cf.wayzer.scriptAgent.Event
import kotlinx.coroutines.Dispatchers
import mindustry.gen.Player
import mindustry.net.Packets.ConnectPacket
import wayzer.lib.dao.PlayerData

/**
 * Call when [ConnectPacket] before new [PlayerData] create
 * This Event is dispatch in [Dispatchers.IO]
 */
class ConnectAsyncEvent(
    val packet: ConnectPacket,
    /** null when new*/
    val data: PlayerData?
) : Event, Event.Cancellable {
    var reason: String? = null
        private set
    override var cancelled: Boolean
        get() = reason != null
        set(@Suppress("UNUSED_PARAMETER") value) {
            error("Can't cancel,please use reject")
        }

    val isNew get() = data == null

    fun reject(reason: String) {
        this.reason = reason
    }

    override val handler = Companion

    companion object : Event.Handler()
}