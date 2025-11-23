@file:Depends("coreMindustry/menu", "调用菜单")
@file:Depends("coreMindustry/util/spawnAround")

/**@author WayZer*/

package mapScript

import arc.util.Align
import coreLibrary.lib.util.loop
import mindustry.game.Team
import mindustry.gen.Iconc
import mindustry.net.Administration
import mindustry.world.blocks.storage.CoreBlock
import org.intellij.lang.annotations.Language

modeIntroduce(
    "招兵买马 CoreWar", """
    点击核心可以打开菜单
    使用铜为队伍购买单位和属性升级
    价格会随购买逐渐变贵
    Tip1: 不要乱花钱被队友嫌弃哦
    Tip2: 单位会随机刷在核心附近(5格左右)，周围没水船会白给
""".trimIndent()
)

@Language("JSON5")
val patch = """
{
    "name": "CoreWar",
    "block.core-foundation.unitType": "alpha",
    "block.core-nucleus.unitType": "alpha",
    "block.core-nucleus.itemCapacity": 1000000,
}
""".trimIndent()
mapPatches = listOf(patch)

data class TeamData(val team: Team) {
    var blockDamageMultiplier by team.rules()::blockDamageMultiplier
    var blockHealthMultiplier by team.rules()::blockHealthMultiplier
    var unitDamageMultiplier by team.rules()::unitDamageMultiplier
    var unitHealthMultiplier by team.rules()::unitHealthMultiplier
}

val teamData = mutableMapOf<Team, TeamData>()
val Team.myData get() = teamData.getOrPut(this) { TeamData(this) }
onDisable { teamData.clear() }

registerActionFilter {
    if(it.type == Administration.ActionType.control || it.type==Administration.ActionType.command){
        if(it.unit.type == UnitTypes.mono)
            return@registerActionFilter false
    }
    true
}

listen<EventType.TapEvent> {
    (it.tile.build as? CoreBlock.CoreBuild)?.let { core ->
        val player = it.player
        if (!player.dead() && core.team == player.team())
            launch(Dispatchers.game) {
                CoreWarMenu(player, core).sendTo(player, 60_000)
            }
    }
}

onEnable {
    state.rules.bannedBlocks.add(Blocks.deconstructor)
    Call.setRules(state.rules)
    loop(Dispatchers.game) {
        delay(2000)
        val teams = Groups.player.mapTo(mutableSetOf()) { it.team() }
        teams.removeAll { !it.active() }
        val text = "[green]点击核心可以打开升级菜单\n" +
                "[yellow]更新：所有单位价格不再增长\n" +
                teams.sortedByDescending { it.myData.unitDamageMultiplier }.take(5)
                    .joinToString("\n") { team ->
                        "[#${team.color}]${team.name}[white]属性：" +
                                "${Iconc.modePvp}${team.myData.unitDamageMultiplier} " +
                                "${team.myData.unitHealthMultiplier} " +
                                "${Iconc.turret}${team.myData.blockDamageMultiplier} " +
                                "${Iconc.defense}${team.myData.blockHealthMultiplier} "
                    }
        Call.infoPopup(
            text, 2.013f,
            Align.topLeft, 350, 0, 0, 0
        )
    }
}