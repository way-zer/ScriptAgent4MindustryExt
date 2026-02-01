@file:Depends("wayzer/maps", "换图")

package wayzer.reGrief

import mindustry.world.blocks.logic.LogicBlock
import wayzer.MapManager
import kotlin.time.Duration.Companion.seconds

//预防一些卡服图，玩家无法复活发起投票

var newMap = false
var counter = 0
onEnable {
    loop(Dispatchers.game) {
        delay(1.seconds)
        if (newMap || Groups.player.count { !it.dead() && it.unit().health > 0 } > 0) {
            counter = 0
            return@loop
        }
        counter += 1
        if (counter > 100) {
            broadcast("[red]无人游玩，5秒后自动换图".with())
            delay(5.seconds)
            MapManager.loadMap()
            newMap = true
        }
    }
}

listen<EventType.ResetEvent> {
    newMap = true
}

listen<EventType.ConnectPacketEvent> {
    //Someone request connect, maybe want to play
    newMap = false
}

listen<EventType.PlayEvent> {
    if (content.blocks().filterIsInstance<LogicBlock>().any { it.maxInstructionsPerTick > 1000 }) {
        broadcast("[yellow]检查到地图含恶意修改，自动换图".with())
        MapManager.loadMap()
    }
}