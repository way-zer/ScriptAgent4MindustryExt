@file:Import("@wayzer/services/MapService", sourceFile = true)
package wayzer.ext

import mindustry.game.EventType
import wayzer.services.MapService
import java.time.Duration

name = "自动Host"

val mapService by ServiceRegistry.nullable<MapService>()
val autoHostTime by config.key(Duration.ofSeconds(5)!!, "自动Host的延迟时间,太短可能服务器未准备就绪")

listen<EventType.ServerLoadEvent> {
    ContentHelper.logToConsole("Auto Host after ${autoHostTime.seconds} seconds")
    launch {
        delay(autoHostTime.toMillis())
        if (net.server()) {//Already host
            ContentHelper.logToConsole("[AutoHost]Already host, pass!")
            return@launch
        }
        mapService ?: ContentHelper.logToConsole("[AutoHost][red]Can't find MapService, pass!")
        mapService?.loadMap()
    }
}