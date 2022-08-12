package wayzer.lib.event

import cf.wayzer.scriptAgent.Event
import kotlinx.coroutines.Dispatchers
import mindustry.net.NetConnection
import mindustry.net.Packets.ConnectPacket
import wayzer.lib.dao.PlayerData

/**
 * Call when [ConnectPacket] before new [PlayerData] create
 * This Event is dispatch in [Dispatchers.IO]
 */
class ConnectAsyncEvent(
    val con: NetConnection,
    val packet: ConnectPacket,
    /** null when new*/
    val data: PlayerData?
) : Event, Event.Cancellable {
    var reason: String? = null
        private set
    override var cancelled: Boolean
        get() = con.kicked
        set(@Suppress("UNUSED_PARAMETER") value) {
            error("Can't cancel,please use con.kick")
        }

    val isNew get() = data == null

    companion object : Event.Handler()
}