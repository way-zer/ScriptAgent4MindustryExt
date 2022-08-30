package wayzer.lib.event

import cf.wayzer.scriptAgent.Event
import mindustry.net.NetConnection
import mindustry.net.Packets.ConnectPacket
import wayzer.lib.dao.PlayerData

/**
 * Call when before [ConnectPacket]
 */
class ConnectAsyncEvent(
    val con: NetConnection,
    val packet: ConnectPacket,
    /** null when new, or create when NormalE*/
    var data: PlayerData?
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