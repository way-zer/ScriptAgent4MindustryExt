@file:Import("@wayzer/user/ext/rank.dao.kt", sourceFile = true)

package wayzer.user.ext

import coreLibrary.DBApi.DB.registerTable
import coreLibrary.lib.util.loop
import mindustry.Vars
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Duration

val enable by config.key(false, "是否开启本插件", "本脚本需要postgreSql作为数据库")
val positions by config.key(emptyMap<String, String>(), "列表名->坐标", "例{经验总榜: \"99,100\"}")
val displayInMap by config.key(false, "在地图中实时显示排行榜", "请先设置好position")

val winWave get() = state.rules.tags.getInt("@winWave", Int.MAX_VALUE)
fun onGameOver(data: Map<PlayerProfile, Pair<Int, Boolean>>) {
    val groupWin = when {
        state.rules.run { pvp || editor || infiniteResources } -> false
        state.rules.attackMode -> state.rules.defaultTeam.active()
        state.rules.waveTimer -> state.wave > winWave
        else -> false
    }
    launch(Dispatchers.IO) {
        data.forEach { (p, d) ->
            if (d.first > 1200) {
                RankData.update({ RankData.id eq p.id.value }) {
                    it.update(played) { played + 1 }
                    if (groupWin || d.second) it.update(win) { win + 1 }
                }
            }
        }
    }
}
onEnable {
    if (!enable)
        return@onEnable ScriptManager.disableScript(this, "本脚本需要postgreSql作为数据库")
    registerTable(RankData)
    export(::onGameOver)
}

fun Player?.showRank(name: String, field: Column<Int>, week: Boolean, display: Boolean) {
    val stmt = RankData.RankStatement(if (!week) field else RankData.weekValue[field]!!, week, this) {
        if (field == RankData.onlineTime) "{value:小时}".with("value" to Duration.ofSeconds(it.toLong()))
        else it
    }
    val result = TransactionManager.current().exec(stmt).orEmpty()
    val msg = "==={name}===\n{list:\n}".with("name" to name, "list" to result)
    if (this?.con == null || !display)
        sendMessage(msg, MsgType.InfoMessage)
    else {
        val pos = positions[name]?.split(',')
        val x = pos?.getOrNull(0)?.toFloatOrNull() ?: 0f
        val y = pos?.getOrNull(1)?.toFloatOrNull() ?: 0f
        launch(Dispatchers.game) {
            val text = "{text}".with("text" to msg).toPlayer(Vars.player)
            Call.label(con, text, 5f, x, y)
        }
    }
}

fun Player?.showRank2(name: String, field: Column<Int>, display: Boolean) {
    showRank(name + "周榜", field, true, display)
    showRank(name + "总榜", field, false, display)
}

fun Player?.showRankAll(display: Boolean) {
    launch(Dispatchers.IO) {
        transaction {
            showRank2("在线时间", RankData.onlineTime, display)
            showRank2("经验", RankData.exp, display)
            showRank2("游玩局数", RankData.played, display)
            showRank2("胜利局数", RankData.win, display)
        }
    }
}

command("rank", "查看排行榜") {
    permission = id.replace('/', '.')
    body {
        player.showRankAll(false)
    }
}

onEnable {
    loop {
        delay(5000L)
        if (displayInMap) {
            Groups.player.forEach {
                it.showRankAll(true)
            }
        }
    }
}