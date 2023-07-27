@file:Depends("coreMindustry/menu", "菜单")
@file:Depends("wayzer/user/ext/statistics", "结算数据")

package wayzer.user.ext

import coreLibrary.DBApi.DB.registerTable
import coreMindustry.MenuBuilder
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Duration

val enable by config.key(false, "是否开启本插件", "本脚本需要postgreSql作为数据库")

onEnable {
    if (!enable) return@onEnable ScriptManager.disableScript(this, "本脚本需要postgreSql作为数据库")
    registerTable(RankData)
}

listenTo<Statistics.GameoverStatisticsEvent> {
    val groupWin = when {
        state.rules.run { pvp || editor || infiniteResources } -> false
        state.rules.attackMode -> state.rules.defaultTeam.active()
        state.rules.waveTimer -> state.wave > state.rules.winWave
        else -> false
    }
    val data = data
    launch(Dispatchers.IO) {
        val withProfile = transaction {
            data.groupBy { PlayerData.findByIdWithTransaction(it.uuid)?.profile }
        }.mapNotNull { (key, value) ->
            (key ?: return@mapNotNull null) to value.maxBy { it.score }
        }
        transaction {
            withProfile.forEach { (p, d) ->
                if (d.score < 1200) return@forEach
                RankData.update({ RankData.id eq p.id.value }) {
                    it.update(played) { played + 1 }
                    if (groupWin || d.win) it.update(win) { win + 1 }
                }
            }
        }
    }
}

val types = mapOf(
    "经验" to RankData.exp,
    "在线时间" to RankData.onlineTime,
    "游玩局数" to RankData.played,
    "胜利局数" to RankData.win,
)
command("rank", "查看排行榜") {
    permission = dotId
    type = CommandType.Client
    body {
        val player = player!!
        var rankType = "经验"
        var weekRank = true
        MenuBuilder {
            title = "排行榜"

            val field = types[rankType]!!.let { if (weekRank) RankData.weekValue[it]!! else it }
            val result = newSuspendedTransaction { exec(RankData.RankStatement(field, weekRank, player)).orEmpty() }
            msg = buildString {
                appendLine("[gold]${rankType}${if (weekRank) "周榜" else "总榜"}[]")
                appendLine()
                result.dropLast(1).forEach {
                    val value = it.value.let { v ->
                        if (rankType != "在线时间") v
                        else "{value:小时}".with("value" to Duration.ofSeconds(v.toLong()))
                    }
                    appendLine("${it.rank.toString().padStart(3)} ${it.name} $value")
                }
                result.last().let {
                    val value = it.value.let { v ->
                        if (rankType != "在线时间") v
                        else "{value:小时}".with("value" to Duration.ofSeconds(v.toLong()))
                    }
                    appendLine("[blue]${it.rank.toString().padStart(3)} ${it.name} $value[]")
                }
            }

            types.forEach { (t, v) ->
                option(t) { rankType = t;refresh() }
            }
            newRow()
            option("周榜") { weekRank = true;refresh() }
            option("总榜") { weekRank = false;refresh() }
            newRow()
            option("关闭") {}
        }.sendTo(player)
    }
}
PermissionApi.registerDefault(dotId, group = "@lvl1")