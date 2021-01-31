package wayzer.lib.event

import cf.wayzer.script_agent.Event
import mindustry.game.Gamemode
import mindustry.game.Rules
import mindustry.maps.Map

/**
 * Call when [wayzer.services.MapService.loadMap]
 * in priority 3,do map change.
 * use [reject] to reject player join server
 */
class MapChange(
    val map: Map,
    mode: Gamemode,
    val applyMode: (Gamemode) -> Rules
) : Event {
    var rules = applyMode(mode)


    override val handler = Companion

    companion object : Event.Handler()
}