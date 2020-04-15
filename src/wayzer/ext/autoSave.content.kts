package wayzer.ext

import mindustry.core.GameState
import mindustry.io.SaveIO
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.schedule

name = "自动存档"
val autoSaveRange = 100 until 106
command("slots", "列出自动保存的存档", type = CommandType.Client) { _, p ->
    val list = autoSaveRange.map { it to SaveIO.fileFor(it) }.filter { it.second.exists() }.map { (id, file) ->
        "[red]{id}[]: [yellow]Save on {date:hh:mm}\n".i18n("id" to id, "date" to file.lastModified().let(::Date))
    }
    p.sendMessage("""
            |[green]===[white] 自动存档 [green]===
            |{list}
            |[green]===[white] {range} [green]===
        """.trimMargin().i18n("range" to autoSaveRange, "list" to list))
}

val nextSaveTime: Date
    get() {//Every 10 minutes
        val t = Calendar.getInstance()
        t.set(Calendar.SECOND, 0)
        val mNow = t.get(Calendar.MINUTE)
        t.add(Calendar.MINUTE, (mNow + 10) / 10 * 10 - mNow)
        return t.time
    }

fun saveMap(task: TimerTask) {
    if (state.`is`(GameState.State.playing)) {
        val minute = ((task.scheduledExecutionTime() / TimeUnit.MINUTES.toMillis(1)) % 60).toInt() //Get the minute
        Core.app.post {
            val id = autoSaveRange.first + minute / 10
            SaveIO.save(SaveIO.fileFor(id))
            broadcast("[green]自动存档完成(整10分钟一次),存档号 [red]{id}".i18n("id" to id))
        }
    }
    SharedTimer.schedule(nextSaveTime, ::saveMap)
}

onEnable {
    SharedTimer.schedule(nextSaveTime, ::saveMap)
}