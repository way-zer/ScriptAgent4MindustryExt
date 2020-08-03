package wayzer.ext

import mindustry.game.EventType
import java.time.Duration

name = "自动Host"

val autoHostTime by config.key(Duration.ofSeconds(5)!!, "自动Host的延迟时间,太短可能服务器未准备就绪")

listen<EventType.ServerLoadEvent> {
    ContentHelper.logToConsole("Auto Host after ${autoHostTime.seconds} seconds")
    launch {
        delay(autoHostTime.toMillis())
        SharedData.mapManager.loadMap()
    }
}