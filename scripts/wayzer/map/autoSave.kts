package wayzer.map

import arc.files.Fi
import arc.struct.StringMap
import coreLibrary.lib.util.loop
import mindustry.core.GameState
import mindustry.io.SaveIO
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.logging.Level

name = "自动存档"
val autoSaveRange = 100 until 106
command("slots", "列出自动保存的存档") {
    body {
        val list = autoSaveRange.map { it to SaveIO.fileFor(it) }.filter { it.second.exists() }.map { (id, file) ->
            "[red]{id}[]: [yellow]Save on {date:hh:mm}".with("id" to id, "date" to file.lastModified().let(::Date))
        }
        reply(
            """
            |[green]===[white] 自动存档 [green]===
            |{list:${"\n"}}
            |[green]===[white] {range} [green]===
        """.trimMargin().with("range" to autoSaveRange, "list" to list)
        )
    }
}

val nextSaveTime: Date
    get() {//Every 10 minutes
        val t = Calendar.getInstance()
        t.set(Calendar.SECOND, 0)
        val mNow = t.get(Calendar.MINUTE)
        t.add(Calendar.MINUTE, (mNow + 10) / 10 * 10 - mNow)
        return t.time
    }

onEnable {
    loop {
        val nextTime = nextSaveTime.time
        delay(nextTime - System.currentTimeMillis())
        if (state.`is`(GameState.State.playing)) {
            val minute = ((nextTime / TimeUnit.MINUTES.toMillis(1)) % 60).toInt() //Get the minute
            Core.app.post {
                val id = autoSaveRange.first + minute / 10
                val tmp = Fi.tempFile("save")
                try {
                    val extTag = StringMap.of(
                        "name", "[回档$id]" + state.map.name(),
                        "description", state.map.description(),
                        "author", state.map.author(),
                    )
                    SaveIO.write(tmp, extTag)
                    tmp.moveTo(SaveIO.fileFor(id))
                } catch (e: Exception) {
                    logger.log(Level.SEVERE, "存档存档出错", e)
                    tmp.delete()
                }
                broadcast("[green]自动存档完成(整10分钟一次),存档号 [red]{id}".with("id" to id))
            }
        }
    }
}