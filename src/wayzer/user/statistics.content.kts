package wayzer.user

import cf.wayzer.placehold.DynamicVar
import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.depends
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.net.Administration
import mindustry.world.Block
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.math.ceil
import kotlin.math.min
import kotlin.reflect.KCallable

data class StatisticsData(
        var playedTime: Int = 0,
        var idleTime: Int = 0,
        var buildScore: Float = 0f,
        var breakBlock: Int = 0,
        var pvpTeam: Team = Team.sharded
) {
    val win get() = state.rules.pvp && pvpTeam == teamWin

    //加权比分
    val score get() = playedTime - 0.7 * idleTime + 0.6 * buildScore + if (win) 1200 else 0

    //结算经验计算
    val exp get() = min(ceil(score * 15 / 3600).toInt(), 25)//3600点积分为15,25封顶

    companion object {
        lateinit var teamWin: Team
    }
}

val Block.buildScore: Float
    get() {
        //如果有更好的建筑积分规则，请修改此处
        return buildCost / 60f //建筑时间(单位秒)
    }
val Player.isIdle get() = velocity().isZero(1e-9F) && !isBuilding() &&!isShooting()

val statisticsData = mutableMapOf<String, StatisticsData>()
val Player.data get() = statisticsData.getOrPut(uuid) { StatisticsData() }
registerVarForType<StatisticsData>().apply {
    registerChild("playedTime", "本局在线时间", DynamicVar { obj, _ -> Duration.ofSeconds(obj.playedTime.toLong()) })
    registerChild("idleTime", "本局在线时间", DynamicVar { obj, _ -> Duration.ofSeconds(obj.idleTime.toLong()) })
    registerChild("buildScore", "建筑积分", DynamicVar { obj, p ->
        if (!p.isNullOrBlank()) p.format(obj.buildScore)
        else obj.buildScore
    })
    registerChild("breakBlock", "破坏方块数", DynamicVar { obj, _ -> obj.breakBlock })
}
onDisable {
    PlaceHoldString.bindTypes.remove(StatisticsData::class.java)//局部类，防止泄漏
}

listen<EventType.ResetEvent> {
    statisticsData.clear()
}
listen<EventType.PlayerJoin> {
    it.player.data.pvpTeam = it.player.team
}

onEnable {
    launch {
        while (true) {
            delay(1000)
            playerGroup.forEach {
                it.data.playedTime++
                if (it.isIdle)
                    it.data.idleTime++
            }
        }
    }
}
listen<EventType.BlockBuildEndEvent> {
    it.player?.data?.apply {
        if (it.breaking)
            breakBlock++
        else
            buildScore += it.tile.block().buildScore
    }
}

listen<EventType.GameOverEvent> { event ->
    val startTime by PlaceHold.reference<Date>("state.startTime")
    val gameTime = Duration.between(startTime.toInstant(), Instant.now())
    if (state.rules.mode() in arrayOf(Gamemode.editor, Gamemode.sandbox)) {
        broadcast("""
            [yellow]本局游戏时长: {gameTime:分钟}
            [yellow]沙盒或编辑器模式,不计算贡献
        """.trimIndent().with("gameTime" to gameTime))
    }

    StatisticsData.teamWin = if (state.rules.mode() != Gamemode.survival) event.winner else Team.sharded
    var totalTime = 0
    val sortedData = statisticsData.filterValues { it.playedTime > 60 }
            .mapKeys { netServer.admins.getInfo(it.key) }
            .toList()
            .sortedByDescending { it.second.score }
    val list = sortedData.map { (player, data) ->
        totalTime += data.playedTime - data.idleTime
        "[white]{pvpState}{player.name}[white]({statistics.playedTime:分钟}/{statistics.idleTime:分钟}/{statistics.buildScore:%.1f}),".with(
                "player" to player, "statistics" to data, "pvpState" to if (data.win) "[green][胜][]" else ""
        )
    }
    broadcast("""
        [yellow]本局游戏时长: {gameTime:分钟}
        [yellow]有效总贡献时长: {totalTime:分钟}
        [yellow]贡献排行榜(时长/挂机/建筑): {list}
    """.trimIndent().with("gameTime" to gameTime, "totalTime" to Duration.ofSeconds(totalTime.toLong()), "list" to list))

    if (sortedData.isNotEmpty() && depends("wayzer/user/expReward") != null && gameTime > Duration.ofMinutes(15)) {
        @Suppress("UNCHECKED_CAST")
        val updateExp = depends("wayzer/user/level").let { it as? IContentScript }
                ?.import<KCallable<*>>("updateExp") as? Player.(Int) -> Boolean
        if (updateExp != null) {
            @OptIn(CacheEntity.NeedTransaction::class)
            transaction {
                val map = mutableMapOf<PlayerProfile, Pair<Administration.PlayerInfo, StatisticsData>>()
                sortedData.groupBy { PlayerData.find(it.first)?.profile }.forEach { (key, value) ->
                    if (key == null || value.isEmpty()) return@forEach
                    map[key] = value.maxBy { it.second.score }!!
                }
                map.forEach { (profile, pair) ->
                    val (player, data) = pair
                    val onlinePlayer = playerGroup.find { it.uuid == player.id }
                    if (onlinePlayer != null) {
                        if (onlinePlayer.updateExp(data.exp))
                            onlinePlayer.sendMessage("[green]经验 +${data.exp}")
                    } else {
                        profile.totalExp += data.exp
                        profile.save()
                    }
                }
            }
        }
    }
}
command("test","",type = CommandType.Client){_,p->
    p!!.sendMessage(p.data.toString())
}