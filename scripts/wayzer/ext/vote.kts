@file:Depends("wayzer/vote", "投票实现")

package wayzer.ext

import arc.Events
import arc.util.Time
import mindustry.game.EventType.GameOverEvent
import wayzer.VoteService
import java.time.Instant
import kotlin.math.ceil
import kotlin.random.Random

fun VoteService.register() {
    addSubVote("投降或结束该局游戏，进行结算", "", "gameOver", "投降", "结算") {
        if (!state.rules.canGameOver)
            returnReply("[red]当前地图不允许投降".with())
        if (state.rules.pvp) {
            val team = player!!.team()
            if (!state.teams.isActive(team) || state.teams.get(team)!!.cores.isEmpty)
                returnReply("[red]队伍已输,无需投降".with())

            start(
                player!!, "投降({team.colorizeName}[yellow]队|要求80%同意)".with("player" to player!!, "team" to team),
                canVote = { it.team() == team }, requireNum = { ceil(it * 0.8).toInt() }
            ) {
                team.data().cores.toArray().forEach {
                    if (it.team == team) it.kill()
                }
            }
            return@addSubVote
        }
        start(player!!, "投降".with(), supportSingle = true) {
            player!!.team().cores().toArray().forEach { Time.run(Random.nextFloat() * 60 * 3, it::kill) }
            Events.fire(GameOverEvent(state.rules.waveTeam))
        }
    }
    addSubVote("快速出波(默认10波,最高50)", "[波数]", "skipWave", "跳波") {
        if (Groups.player.any { it.team() == state.rules.waveTeam })
            returnReply("[red]当前模式禁止跳波".with())
        val lastResetTime by PlaceHold.reference<Instant>("state.startTime")
        val t = (arg.firstOrNull()?.toIntOrNull() ?: 10).coerceIn(1, 50)
        start(player!!, "跳波({t}波)".with("t" to t), supportSingle = true) {
            val startTime = Instant.now()
            var waitTime = 3
            repeat(t) {
                while (state.enemies > 300) {//延长等待时间
                    if (waitTime > 60) return@start //等待超时
                    delay(waitTime * 1000L)
                    waitTime *= 2
                }
                if (lastResetTime > startTime) return@start //Have change map
                Core.app.post { logic.runWave() }
                delay(waitTime * 1000L)
            }
        }
    }
    addSubVote("清理本队建筑记录", "", "clear", "清理", "清理记录") {
        val team = player!!.team()
        start(player!!, "清理建筑记录({team.colorizeName}[yellow]队|需要2/5同意)".with("team" to team),
            canVote = { it.team() == team }, requireNum = { ceil(it * 0.4).toInt() }
        ) {
            team.data().plans.clear()
        }
    }
    addSubVote("自定义投票", "<内容>", "text", "文本", "t") {
        if (arg.isEmpty()) returnReply("[red]请输入投票内容".with())
        start(player!!, "自定义([green]{text}[yellow])".with("text" to arg.joinToString(" "))) {}
    }
}

onEnable {
    VoteService.register()
}