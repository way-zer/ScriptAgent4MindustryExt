@file:Depends("coreMindustry/utilMapRule", "修改单位和建筑属性")

package mapScript

import arc.util.Align
import arc.util.Time
import coreLibrary.lib.util.loop
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.world.blocks.liquid.Conduit
import mindustry.world.blocks.storage.CoreBlock

/**@author xkldklp
 * [https://mdt.wayzer.top/v2/map/13554/latest]
 */
name = "母舰-撤离-普通"


onEnable {
    contextScript<coreMindustry.UtilMapRule>().apply {
        registerMapRule((Blocks.coreShard as CoreBlock)::unitType) { UnitTypes.mono }
        registerMapRule((Blocks.coreFoundation as CoreBlock)::unitType) { UnitTypes.mono }
        registerMapRule((Blocks.coreNucleus as CoreBlock)::unitType) { UnitTypes.mono }
        registerMapRule((Blocks.conduit as Conduit)::health) { 1 }
    }
    Groups.player.each { it.clearUnit() }

    val startTime = Time.millis()
    fun lvl() = Time.timeSinceMillis(startTime) / 1000f
    listen(EventType.Trigger.update) {
        //10MIN后增加强度加倍
        val damageBase = 6 + 0.025f * lvl() + 0.5f * (lvl() - 600).coerceAtLeast(0f)
        Groups.unit.each { u ->
            if (u.team != Team.crux) {
                u.damageContinuous(damageBase / 60f)
                if (!u.isPlayer && u.health <= lvl() * 0.15f) {
                    u.team = Team.crux
                }
            }
        }
    }
    loop(Dispatchers.game) {
        delay(2000)
        Call.infoPopup("机械干扰强度:${lvl() * 0.15f}\n[red]生命低于此值的非玩家单位会被直接转化为红色单位", 2.013f, Align.topLeft, 350, 0, 0, 0)
    }
}

