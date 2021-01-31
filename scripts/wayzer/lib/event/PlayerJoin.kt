package wayzer.lib.event

import cf.wayzer.script_agent.Event
import mindustry.gen.Player
import wayzer.lib.dao.PlayerData

/**
 * Call when [mindustry.game.EventType.PlayerConnect] before new [PlayerData] create
 * use [reject] to reject player join server
 */
class PlayerJoin(
    val player: Player,
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