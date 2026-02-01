package wayzer.map

import cf.wayzer.scriptAgent.Event
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player

data class AssignTeamEvent(val player: Player, val group: Iterable<Player>, val oldTeam: Team?) : Event,
    Event.Cancellable {
    var team: Team? = oldTeam
        set(value) {
            field = value
            cancelled = true
        }
    override var cancelled: Boolean = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler() {
        val spectateTeam = Team.all[255]!!
    }
}

interface TeamService {
    val allTeam: Set<Team>
    var bannedTeam: Set<Team>
    fun updateBannedTeam(force: Boolean = false)
    fun randomTeam(player: Player, group: Iterable<Player> = Groups.player): Team
    fun changeTeam(p: Player, team: Team = randomTeam(p))
}