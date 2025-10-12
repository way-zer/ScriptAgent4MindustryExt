@file:Depends("wayzer/map/betterTeam", "队伍分配")

package mapScript.shared

import mindustry.world.blocks.storage.CoreBlock.CoreBuild
import mindustry.world.modules.ItemModule
import kotlin.time.Duration.Companion.minutes

listen<EventType.BlockDestroyEvent> {
    val core = it.tile.build as? CoreBuild ?: return@listen
    core.items = ItemModule() //防止爆炸
    launch(Dispatchers.gamePost) {
        HexData.pos2hex[core.pos()]?.occupy(core.lastDamage)
    }
}
listen<EventType.BlockBuildEndEvent> {
    val core = it.tile.build as? CoreBuild ?: return@listen
    launch(Dispatchers.gamePost) {
        HexData.pos2hex[core.pos()]?.occupy(core.team)
    }
}

listenTo<wayzer.map.BetterTeam.AssignTeamEvent> {
    team = HexData.assignTeam(player, group)
}

onEnable {
    launch(Dispatchers.game) {
        delay(1.minutes)
        state.rules.canGameOver = true
    }
}

onDisable {
    HexData.reset()
}