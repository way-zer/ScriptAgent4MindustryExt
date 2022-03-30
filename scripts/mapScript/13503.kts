package mapScript

import arc.util.Align
import arc.util.Time
import coreLibrary.lib.util.loop
import mindustry.game.Team

/**@author xkldklp WayZer*/

onEnable {
    val startTime = Time.millis()
    val rawBlockHealth = Team.crux.rules().blockHealthMultiplier
    val rawUnitDamage = Team.crux.rules().unitDamageMultiplier
    loop(Dispatchers.game) {
        delay(2000)
        Team.crux.rules().apply {
            blockHealthMultiplier =
                (rawBlockHealth - Time.timeSinceMillis(startTime) / 1000 * 0.002f)
                    .coerceAtLeast(0.1f)
            unitDamageMultiplier = (rawUnitDamage - Time.timeSinceMillis(startTime) / 1000 * 0.001f).coerceAtLeast(0.1f)
        }
        Call.infoPopup(
            "[violet]红队建筑血量: [orange]${Team.crux.rules().blockHealthMultiplier}\n" +
                    "[violet]红队单位攻击: [orange]${Team.crux.rules().unitDamageMultiplier}",
            2.013f, Align.topLeft, 350, 0, 0, 0
        )
    }
}

listen<EventType.UnitDestroyEvent> { e ->
    if (e.unit.team != Team.crux) {
        e.unit.type.spawn(Team.crux, e.unit.x, e.unit.y).health = e.unit.type.health * 0.4f
    }
}