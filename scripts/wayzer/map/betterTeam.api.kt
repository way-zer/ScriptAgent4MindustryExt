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
    /** 当前可供玩家使用的全部队伍 */
    val allTeam: Set<Team>
    /** 当前被规则封禁的队伍 */
    var bannedTeam: Set<Team>
    /** 存储队伍，用于下一次[randomTeam] */
    val savedTeams: MutableMap<String, Team>
    /** 根据Tag更新[bannedTeam]，将自动为被ban玩家换队 */
    fun updateBannedTeam(force: Boolean = false)
    /** 为玩家随机分配队伍 */
    fun randomTeam(player: Player, group: Iterable<Player> = Groups.player): Team
    /** 切换玩家到指定[team],或[randomTeam] */
    fun changeTeam(p: Player, team: Team = randomTeam(p))
}