@file:Depends("mapScript/tags/TDDrop", "掉落", soft = true)

package mapScript.tags

import arc.math.geom.Geometry
import mindustry.net.Administration
import mindustry.world.blocks.ConstructBlock
import mindustry.world.blocks.environment.Floor

/** 塔防模式
 * @author WayZer
 * 如果有修改建议，建议提交PR，维护社区统一。
 * */

registerMapTag("@towerDefend")
modeIntroduce(
    "塔防模式", """
    1.怪物只会攻击核心,可以放心建筑
    2.过于靠近怪物的兵可能会被杀掉
    3.道路上不准建筑(原因由上)
    4.核心附加的道路允许放置武装传送带
    5.怪物会掉落大量战利品(不限距离)
    6.部分物品被禁用(保证平衡)
""".trimIndent()
)

val allowBlocks = arrayOf(Blocks.armoredConveyor, Blocks.plastaniumConveyor)
val floors = mutableSetOf<Floor>()
onEnable {
    state.rules.bannedBlocks.takeIf { it.isEmpty }?.apply {
        add(Blocks.arc)
        add(Blocks.lancer)
        add(Blocks.airFactory)
        add(Blocks.mendProjector)
    }
    for (tile in spawner.spawns) {
        Geometry.circle(0, 0, 4) { dx, dy ->
            floors += tile.nearby(dx, dy)?.floor() ?: return@circle
        }
    }
}
onDisable { floors.clear() }

//build

registerActionFilter {
    when (it.type) {
        Administration.ActionType.placeBlock -> {
            if (it.block in allowBlocks && it.player.team().cores().any { core -> core.dst(it.tile) < 80 }) {
                return@registerActionFilter true //允许在核心附近建武装传送带
            }
            (it.tile.floor() !in floors).also { b ->
                if (!b) it.player.sendMessage("[red]你不能在此处建造".with(), MsgType.InfoToast, 3f)
            }
        }

        Administration.ActionType.configure -> {
            when {
                it.tile.block() == Blocks.itemSource || it.tile.block() == Blocks.liquidSource -> {
                    it.player.sendMessage("[red]不允许修改源".with(), MsgType.InfoToast, 3f)
                    return@registerActionFilter false
                }
            }
            true
        }

        else -> true
    }
}
listen<EventType.BlockBuildBeginEvent> {
    if (it.breaking) return@listen
    if (it.tile.floor() in floors) {
        if (it.unit.isPlayer) {
            return@listen//limited by filter
        }
        it.tile.remove()
    }
}
listen<EventType.TileChangeEvent> {
    val tile = it.tile
    if (tile.block() == Blocks.air || tile.block() in allowBlocks || tile.floor() !in floors) return@listen
    val building = tile.build
    if (building is ConstructBlock.ConstructBuild && building.current in allowBlocks)
        return@listen
    launch(Dispatchers.gamePost) {
        Call.deconstructFinish(tile, Blocks.air, null)
    }
}

//unit

val specialFlag = 1024.0//use for identify unit spawned from factory
listen<EventType.UnitCreateEvent> {
    if (it.unit.team == state.rules.waveTeam)
        it.unit.flag = specialFlag
}
listen(EventType.Trigger.update) {
    val units = state.rules.waveTeam.data().units
    units.forEach {
        if (it.flag == specialFlag) return@forEach
        if (it.controller() !is TowerDefendAI)
            it.controller(TowerDefendAI(floors))
    }
}