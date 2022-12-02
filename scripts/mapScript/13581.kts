@file:Depends("coreMindustry/utilMapRule", "修改核心单位")

package mapScript

import arc.Events
import arc.util.Align
import arc.util.Interval
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.game.Team
import mindustry.gen.Unit
import mindustry.world.blocks.storage.CoreBlock

/**@author xkldklp
 * ClearUp: WayZer
 * [https://mdt.wayzer.top/v2/map/13581/latest] */
name = "极限追猎V-[purple]终末神域"


fun Unit.setHealth(Health: Float) {
    health = Health
    maxHealth = Health

}

var stage = 0
val interval = Interval(2)
suspend fun loopUtilKill(shieldCD: Float, coreDamageCD: Float) {
    stage++
    while (Team.crux.data().unitCount > 0) {
        if (interval[0, shieldCD * 60f]) {
            Team.crux.data().units.each {
                it.shield += it.maxHealth * 0.1f
            }
        }
        if (interval[1, coreDamageCD * 60f]) {
            Team.sharded.core().damage(Team.crux, 99999f)
        }

        val text = buildString {
            appendLine("[cyan]第${stage}阶段[red]BOSS[white]")
            Team.crux.data().units.sortedBy { it.health + it.shield }.forEach {
                appendLine("[red]${it.type}: [green]\uE813[white]${it.health} + [green]\uE84D[white]${it.shield}")
            }
            appendLine("[yellow]\uE84D[white]${shieldCD - interval.getTime(0) / 60f}")
            appendLine("[red]⚠[white]${coreDamageCD - interval.getTime(1) / 60f}")
        }
        Call.infoPopup(
            text, 2.013f,
            Align.topLeft, 350, 0, 0, 0
        )
        delay(2000L)
    }
}

onEnable {
    contextScript<coreMindustry.UtilMapRule>().apply {
        registerMapRule((Blocks.coreShard as CoreBlock)::unitType) { UnitTypes.nova }
        registerMapRule((Blocks.coreFoundation as CoreBlock)::unitType) { UnitTypes.pulsar }
        registerMapRule((Blocks.coreNucleus as CoreBlock)::unitType) { UnitTypes.quasar }
    }
    launch(Dispatchers.game) {
        run {
            UnitTypes.eclipse.spawn(Team.crux, 150 * 8f, 150 * 8f).setHealth(44000f)//地图中心生成一只死神
            Team.crux.rules().blockHealthMultiplier = 5f
            Team.crux.rules().unitDamageMultiplier = 0.5f
            loopUtilKill(120f, 60f)
        }
        run {
            Call.announce("不错")
            delay(2000L)
            Call.announce("但是战斗..才刚刚开始哦")
            delay(2000L)
            UnitTypes.eclipse.spawn(Team.crux, 150 * 8f, 150 * 8f).setHealth(88000f)
            Team.crux.rules().blockHealthMultiplier = 3f
            Team.crux.rules().unitDamageMultiplier = 1.5f
            loopUtilKill(60f, 25f)
        }
        run {
            Call.announce("啧")
            delay(2000L)
            Call.announce("我承认，你很强")
            delay(2000L)
            Call.announce("但我")
            delay(1000L)
            Call.announce("\n更强!")
            delay(1000L)
            UnitTypes.eclipse.spawn(Team.crux, 150 * 8f, 150 * 8f).setHealth(144000f)
            UnitTypes.toxopid.spawn(Team.crux, 150 * 8f, 150 * 8f).setHealth(44000f)
            Team.crux.rules().blockHealthMultiplier = 2f
            Team.crux.rules().unitDamageMultiplier = 3f
            loopUtilKill(80f, 12f)
        }
        run {
            Call.announce("看来有必要让你见识一下")
            delay(2000L)
            Call.announce("[red]来自终末神域的\n[purple]绝对力量了")//几把中二台词
            delay(10000L)
            repeat(2) {
                UnitTypes.eclipse.spawn(Team.crux, 150 * 8f, 150 * 8f).setHealth(44000f)
            }
            UnitTypes.toxopid.spawn(Team.crux, 150 * 8f, 150 * 8f).setHealth(288000f)
            Team.crux.rules().blockHealthMultiplier = 0.5f
            Team.crux.rules().unitDamageMultiplier = 6f
            loopUtilKill(50f, 5f)
        }

        Call.announce("....")
        delay(2000L)
        Call.announce("你赢了")
        state.gameOver = true
        Events.fire(EventType.GameOverEvent(Team.sharded))
    }
}

