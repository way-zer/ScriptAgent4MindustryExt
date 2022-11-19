@file:Depends("wayzer/maps", "地图管理")
@file:Depends("wayzer/voteService", "投票实现")
@file:Depends("coreMindustry/utilNext")
@file:Depends("wayzer/user/ext/skills")
@file:Depends("wayzer/user/ext/skillsC")
@file:Depends("wayzer/map/betterTeam")

package wayzer.ext

import arc.Core
import arc.Events
import arc.util.Time
import coreLibrary.DBApi.DB.registerTable
import coreLibrary.lib.PermissionApi
import coreLibrary.lib.PlaceHold
import coreLibrary.lib.with
import coreMindustry.lib.*
import mindustry.Vars
import mindustry.Vars.*
import mindustry.core.NetServer
import mindustry.game.EventType
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.io.SaveIO
import mindustry.world.blocks.storage.CoreBlock
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.javatime.CurrentTimestamp
import org.jetbrains.exposed.sql.javatime.timestamp
import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.MapManager
import wayzer.MapRegistry
import wayzer.VoteService
import wayzer.ext.Vote.AssignTeamEvent
import wayzer.lib.dao.PlayerData
import wayzer.lib.dao.PlayerProfile
import wayzer.lib.dao.util.NeedTransaction
import java.text.DateFormat
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.min
import kotlin.random.Random


val voteService = contextScript<VoteService>()


class PlayerObserver(id: EntityID<Int>) : IntEntity(id) {
    var profileId by T.profile
    var reason by T.reason
    val createTime by T.createTime
    var endTime by T.endTime
//    var undo by T.undo

    object T : IntIdTable("PlayerObserver") {
        val profile = reference("profile", PlayerProfile.T)
        val reason = text("reason", eagerLoading = true)
        val createTime = timestamp("createTime").defaultExpression(CurrentTimestamp())
        val endTime = timestamp("endTime").defaultExpression(CurrentTimestamp())
//        val undo = bool("undo").default(false)
    }

    companion object : IntEntityClass<PlayerObserver>(T) {
        @NeedTransaction
        fun create(profile: PlayerProfile, time: Duration, reason: String): PlayerObserver {
            findNotEnd(profile.id)?.let { return it }
            return PlayerObserver.new {
                profileId = profile.id
                endTime = Instant.now() + time
                this.reason = reason
            }
        }

        @NeedTransaction
        fun allNotEnd() = PlayerObserver.find(T.endTime.greater(CurrentTimestamp()))

        @NeedTransaction
        fun findNotEnd(profileId: EntityID<Int>): PlayerObserver? {
            return PlayerObserver.find { (T.profile eq profileId) and (T.endTime.greater(CurrentTimestamp())) }
                .firstOrNull()
        }
    }
}

data class AssignTeamEvent(val player: Player, val group: Iterable<Player>, val oldTeam: Team?) : Event,
    Event.Cancellable {
    var team: Team? = oldTeam
        set(value) {
            field = value
            cancelled = true
        }
    override var cancelled: Boolean = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

data class ChangeTeamEvent(val player: Player, var team: Team) : Event {
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

val spectateTeam = Team.all[255]!!
val allTeam: Set<Team>
    get() = state.teams.getActive().mapTo(mutableSetOf()) { it.team }.apply {
        remove(Team.derelict)
        removeIf { !it.data().hasCore() }
        removeAll(bannedTeam)
    }

@Savable(false)
val teams = mutableMapOf<String, Team>()
customLoad(::teams, teams::putAll)
var bannedTeam = emptySet<Team>()
var keepTeamsOnce = false

onEnable {
    val backup = netServer.assigner
    netServer.assigner = NetServer.TeamAssigner { p, g ->
        ChangeTeamEvent(p, randomTeam(p, g)).emit().team.also {
            teams[p.uuid()] = it
        }
    }
    onDisable { netServer.assigner = backup }
    updateBannedTeam(true)
}
listen<EventType.PlayEvent> { updateBannedTeam(true) }
//custom gameover
listen<EventType.BlockDestroyEvent> { e ->
    if (state.gameOver || !state.rules.pvp) return@listen
    if (e.tile.block() is CoreBlock)
        launch(Dispatchers.gamePost) {
            if (state.gameOver) return@launch
            allTeam.singleOrNull()?.let {
                state.gameOver = true
                Events.fire(EventType.GameOverEvent(it))
            }
        }
}
listen<EventType.ResetEvent> {
    if (keepTeamsOnce) {
        keepTeamsOnce = false
        return@listen
    }
    teams.clear()
}

fun updateBannedTeam(force: Boolean = false) {
    if (force || bannedTeam.isEmpty())
        bannedTeam = state.rules.tags.get("@banTeam")?.split(',').orEmpty()
            .mapNotNull { Team.all.getOrNull(it.toIntOrNull() ?: -1) }.toSet()
    Groups.player.filter { it.team() in bannedTeam }.forEach {
        changeTeam(it)
        it.sendMessage("[yellow]因为原队伍被禁用,你已自动切换队伍".with(), MsgType.InfoMessage)
    }
}

/**
 * 1. 触发[AssignTeamEvent]
 * 2. 尝试使用[teams]队伍
 * 3. 从[allTeam]随机分配队伍
 */
fun randomTeam(player: Player, group: Iterable<Player> = Groups.player): Team {
    val allTeam = allTeam
    if (teams[player.uuid()]?.run { this != spectateTeam && this !in allTeam } == true)
        teams.remove(player.uuid())
    val fromEvent = AssignTeamEvent(player, group, teams[player.uuid()]).emit().team
    if (fromEvent != null) return fromEvent
    if (!state.rules.pvp) return state.rules.defaultTeam
    return allTeam.shuffled()
        .minByOrNull { group.count { p -> p.team() == it && player != p } }
        ?: state.rules.defaultTeam
}

fun changeTeam(p: Player, team: Team = randomTeam(p)) {
    val newTeam = ChangeTeamEvent(p, team).emit().team
    teams[p.uuid()] = newTeam
    p.clearUnit()
    p.team(newTeam)
    p.clearUnit()
}

fun VoteService.register() {
    addSubVote("换图投票", "<地图ID> [网络换图类型参数]", "map", "换图") {
        if (arg.isEmpty())
            returnReply("[red]请输入地图序号".with())
        launch(Dispatchers.game) {
            val map = arg[0].toIntOrNull()?.let { MapRegistry.findById(it, reply) }
                ?: return@launch reply("[red]地图序号错误,可以通过/maps查询".with())
            start(
                player!!,
                "换图([green]{nextMap.id}[]: [green]{nextMap.map.name}[yellow]|[green]{nextMap.mode}[])".with("nextMap" to map),
                supportSingle = true
            ) {
                if (map.map.file.exists() && !SaveIO.isSaveValid(map.map.file))
                    return@start broadcast("[red]换图失败,地图[yellow]{nextMap.name}[green](id: {nextMap.id})[red]已损坏".with("nextMap" to map))
                MapManager.loadMap(map)
                Core.app.post { // 推后,确保地图成功加载
                    broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".with())
                }
            }
        }
    }
    addSubVote("投降或结束该局游戏，进行结算", "", "gameOver", "投降", "结算") {
        if (!state.rules.canGameOver)
            returnReply("[red]当前地图不允许投降".with())
        if (state.rules.pvp) {
            val team = player!!.team()
            if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty)
                returnReply("[red]队伍已输,无需投降".with())

            canVote = canVote.let { default -> { default(it) && it.team() == team } }
            start(player!!, "投降({team.colorizeName}[yellow]队|需要全队同意)".with("player" to player!!, "team" to team)) {
                state.teams.get(team).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
            }
        }
        start(player!!, "投降".with(), supportSingle = true) {
            state.teams.get(player!!.team()).cores.forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
        }
    }
    addSubVote("快速出波(默认10波,最高50)", "[波数]", "skipWave", "跳波") {
        val lastResetTime by PlaceHold.reference<Instant>("state.startTime")
        val t = min(arg.firstOrNull()?.toIntOrNull() ?: 10, 50)
        start(player!!, "跳波({t}波)".with("t" to t), supportSingle = true) {
            launch {
                val startTime = Instant.now()
                var waitTime = 3
                repeat(t) {
                    while (state.enemies > 300) {//延长等待时间
                        if (waitTime > 60) return@launch //等待超时
                        delay(waitTime * 1000L)
                        waitTime *= 2
                    }
                    if (lastResetTime > startTime) return@launch //Have change map
                    Core.app.post { logic.runWave() }
                    delay(waitTime * 1000L)
                }
            }
        }
    }
    addSubVote("回滚到某个存档(使用/slots查看)", "<存档ID>", "rollback", "load", "回档") {
        if (arg.firstOrNull()?.toIntOrNull() == null)
            returnReply("[red]请输入正确的存档编号".with())
        val map = MapManager.getSlot(arg[0].toInt())
            ?: returnReply("[red]存档不存在或存档损坏".with())
        start(player!!, "回档".with(), supportSingle = true) {
            MapManager.loadSave(map)
            broadcast("[green]回档成功".with(), quite = true)
        }
    }
    //ban
    fun ban(uuid: String, time: Int, reason: String) {
        launch(Dispatchers.IO) {
            withContext(Dispatchers.game) {
                Groups.player.find { it.uuid() == uuid }?.let {
                    if (it.uuid() == "FzCaAjh/Do8AAAAA9LRoUA==") {
                        broadcast("[sky]被踢的对象是cong,由于测试用途,无法踢出".with())
                        return@let
                    }
                    it.kick(
                        if (reason != "") {
                            reason
                        } else {
                            "你已被投票踢出"
                        }, time.toLong()
                    )
                    if (reason == "") {
                        broadcast("[red]投票禁封了{target.name}".with("target" to it))
                    } else {
                        broadcast(
                            "[red]投票禁封了{target.name},原因: [yellow]{reason}".with(
                                "target" to it,
                                "reason" to reason
                            )
                        )
                    }
                }
            }
        }
    }
    //ob
    registerTable(PlayerObserver.T)
    fun observer(uuid: String, time: Int, reason: String) {
        launch(Dispatchers.IO) {
            Groups.player.find { it.uuid() == uuid }?.let {
                /*if (it.uuid()=="FzCaAjh/Do8AAAAA9LRoUA=="){
                    broadcast("[sky]被踢的对象是cong,由于测试用途,无法ob".with())
                    return@let
                }*/
                changeTeam(it, spectateTeam)
                if (reason == "") {
                    broadcast("[red]投票ob了{target.name}".with("target" to it))
                } else {
                    broadcast(
                        "[red]投票ob了{target.name},原因: [yellow]{reason}".with(
                            "target" to it,
                            "reason" to reason
                        )
                    )
                }
            }
        }
    }
    listen<EventType.PlayerConnect> {
        val profile = PlayerData.findById(it.player.uuid())?.profile ?: return@listen
        launch(Dispatchers.IO) {
            val ob = transaction { PlayerObserver.findNotEnd(profile.id) } ?: return@launch
            withContext(Dispatchers.game) {
                changeTeam(it.player, spectateTeam)
            }
        }
    }
    listen<ChangeTeamEvent> {
        val profile = PlayerData.findById(it.player.uuid())?.profile ?: return@listen
        launch(Dispatchers.IO) {
            val ob = transaction { PlayerObserver.findNotEnd(profile.id) } ?: return@launch
            withContext(Dispatchers.game) {
                changeTeam(it.player, spectateTeam)
            }
        }
    }
    //uuid -> ?
    fun uname(uuid: String): String? {
        Groups.player.find { it.uuid() == uuid }?.let {
            return it.name
        }
        return ""
    }

    fun up(uuid: String): Player {
        Groups.player.find { it.uuid() == uuid }?.let {
            return it
        }
        return Vars.player
    }

    addSubVote("踢出某人15分钟", "<玩家名/3位id> <踢人理由>", "kick", "踢出") {
        val uuid = netServer.admins.getInfoOptional(arg[0])?.id
            ?: depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(arg[0])
            ?: Groups.player.find { it.name == arg.joinToString(" ") }.uuid()
            ?: returnReply("[red]请输入目标正确的3位ID，位于名字后面".with())
        val time: Int = 15 * 60 * 1000
        val reason = arg.slice(1 until arg.size).joinToString("")
        start(player!!,if(reason == ""){"踢人(踢出[red]{name}[yellow]"}else{"\n踢出[red]{name}[yellow]\n原因是[sky]{reason}\n"}.with("reason" to reason,"name" to uname(uuid).toString())) {
            ban(uuid, time, reason)
            reply("[green]已禁封{name},[sky]原因：{reason}".with("name" to uname(uuid).toString(), "reason" to reason))
        }
    }
    addSubVote("清理本队建筑记录", "", "clear", "清理", "清理记录") {
        val team = player!!.team()

        canVote = canVote.let { default -> { default(it) && it.team() == team } }
        start(player!!, "清理建筑记录({team.colorizeName}[yellow]队)".with("team" to team)) {
            team.data().plans.clear()
        }
    }
    addSubVote("自定义投票", "<内容>", "text", "文本", "t") {
        if (arg.isEmpty()) returnReply("[red]请输入投票内容".with())
        start(player!!, "自定义([green]{text}[yellow])".with("text" to arg.joinToString(" "))) {}
    }
    addSubVote("实验性强制ob", "<玩家名/3位id>", "ob") {
        val uuid = netServer.admins.getInfoOptional(arg[0])?.id
            ?: depends("wayzer/user/shortID")?.import<(String) -> String?>("getUUIDbyShort")?.invoke(arg[0])
            ?: Groups.player.find { it.name == arg.joinToString(" ") }.uuid()
            ?: returnReply("[red]请输入目标正确的3位ID，位于名字后面".with())
        val time: Int = 60 * 1000
        val reason = arg.slice(1 until arg.size).joinToString("")
        start(player!!,if(reason == ""){"[red]ob了[red]{name}[yellow]"}else{"\nob[red]{name}[yellow]\n原因是[sky]{reason}\n"}.with("reason" to reason,"name" to uname(uuid).toString())) {
            observer(uuid, time, reason)
            //reply(if(reason == ""){"[red]ob了[red]{name}[yellow]"}else{"\nob[red]{name}[yellow]\n原因是[sky]{reason}\n"}.with("reason" to reason,"name" to uname(uuid).toString()))
        }
    }
}

onEnable {
    voteService.register()
}

PermissionApi.registerDefault("wayzer.vote.*")


command("ob", "切换为观察者") {
    type = CommandType.Client
    permission = "wayzer.ext.observer"
    body {
        if (player!!.team() == spectateTeam) {
            teams.remove(player!!.uuid())
            changeTeam(player!!)
            broadcast(
                "[yellow]玩家[green]{player.name}[yellow]重新投胎到{player.team.colorizeName}"
                    .with("player" to player!!), type = MsgType.InfoToast, quite = true
            )
        } else {
            changeTeam(player!!, spectateTeam)
            broadcast(
                "[yellow]玩家[green]{player.name}[yellow]选择成为观察者"
                    .with("player" to player!!), type = MsgType.InfoToast, quite = true
            )
            player!!.sendMessage("[green]再次输入指令可以重新投胎")
        }
    }
}

