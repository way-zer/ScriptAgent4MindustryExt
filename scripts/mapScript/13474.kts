package mapScript

import mindustry.content.UnitTypes
import mindustry.game.Team
import mindustry.gen.Unit

/**@author WayZer*/

onEnable {
    //origin js from map 13474
    val gamma = UnitTypes.gamma
    fun Unit.applyAsGamma() {
        type = gamma
        mounts = type.create(Team.derelict).mounts()
        health = gamma.health * 2
        maxHealth = gamma.health * 2
    }
    repeat(15) {
        UnitTypes.oxynoe.spawn(Team.sharded, 14 * 8f, 7 * 8f).applyAsGamma()
        UnitTypes.oxynoe.spawn(Team.crux, 14 * 8f, 13 * 8f).applyAsGamma()
    }
    Team.get(1).rules().blockHealthMultiplier = 1E12f
    Team.get(2).rules().blockHealthMultiplier = 1E12f
}