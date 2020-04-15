package wayzer.ext

import mindustry.game.EventType
import java.time.Duration
import kotlin.concurrent.schedule

name = "自动Host"

val autoHostTime by config.key(Duration.ofSeconds(5)!!, "自动Host的延迟时间,太短可能服务器未准备就绪")

listen<EventType.ServerLoadEvent> {
    ContentHelper.logToConsole("Auto Host after ${autoHostTime.seconds} seconds")
    SharedTimer.schedule(autoHostTime.toMillis()) {
        SharedData.mapManager.loadMap()
    }
}