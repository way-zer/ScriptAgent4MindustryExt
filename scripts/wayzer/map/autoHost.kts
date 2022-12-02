@file:Depends("wayzer/maps", "地图管理")

package wayzer.map

import coreLibrary.lib.config
import coreMindustry.lib.ContentHelper
import coreMindustry.lib.listen
import mindustry.game.EventType
import wayzer.MapManager
import java.time.Duration

name = "自动Host"

val autoHostTime by config.key(Duration.ofSeconds(5)!!, "自动Host的延迟时间,太短可能服务器未准备就绪")

listen<EventType.ServerLoadEvent> {
    ContentHelper.logToConsole("Auto Host after ${autoHostTime.seconds} seconds")
    launch {
        delay(autoHostTime.toMillis())
        if (net.server()) {//Already host
            ContentHelper.logToConsole("[AutoHost]Already host, pass!")
            return@launch
        }
        MapManager.loadMap()
    }
}