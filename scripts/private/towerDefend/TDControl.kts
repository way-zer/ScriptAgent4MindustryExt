package private.towerDefend

/**
 * 该脚本为私有脚本，归WayZer所有
 * 未经许可，禁止转让给他人或者用作其他用途
 */
import arc.Core
import arc.math.geom.Geometry
import cf.wayzer.placehold.DynamicVar
import coreLibrary.lib.registerVar
import coreLibrary.lib.with
import coreMindustry.lib.MsgType
import coreMindustry.lib.listen
import coreMindustry.lib.registerActionFilter
import coreMindustry.lib.sendMessage
import mindustry.Vars.spawner
import mindustry.Vars.state
import mindustry.content.Blocks
import mindustry.core.GameState
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Nulls
import mindustry.gen.Player
import mindustry.net.Administration
import mindustry.world.blocks.ConstructBlock
import mindustry.world.blocks.environment.Floor


registerVar("TDInfoDisplay", "塔防信息展示", DynamicVar.v {
    if (!tdMode) ""
    else "[violet]塔防难度系数: [orange]{multiplier}\n".with(
        "multiplier" to (getVar("TDMultiplier") ?: 1)
    )
})

val tdMode get() = state.rules.tags.containsKey("@towerDefend")
val allowBlocks = arrayOf(Blocks.armoredConveyor, Blocks.plastaniumConveyor)

val floors = mutableSetOf<Floor>()
registerActionFilter {
    if (!tdMode) return@registerActionFilter true
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
                    val old = it.tile.build.config()
                    Core.app.post { it.tile?.build?.configureAny(old) }
                }
            }
            return@registerActionFilter true
        }

        else -> true
    }
}
listen<EventType.BlockBuildBeginEvent> {
    if (!tdMode || it.breaking) return@listen
    if (it.tile.floor() in floors) {
        if (it.unit.isPlayer) {
            return@listen//limited by filter
        }
        it.tile.remove()
    }
}

listen<EventType.TileChangeEvent> {
    if (!tdMode || it.tile.block() == Blocks.air || it.tile.block() in allowBlocks) return@listen
    val building = it.tile.build
    if (building is ConstructBlock.ConstructBuild && building.current in allowBlocks)
        return@listen
    if (it.tile.floor() in floors) {
        Core.app.post {
            Call.deconstructFinish(it.tile, Blocks.air, Nulls.unit)
        }
    }
}

listen<EventType.ResetEvent> { floors.clear() }
fun trySetRule() {
    if (!tdMode) return
    if (!state.rules.tags.containsKey("@TDDrop"))
        state.rules.tags.put("@TDDrop", "true")
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
listen<EventType.PlayEvent> { Core.app.post(::trySetRule) }
onEnable {
    if (state.`is`(GameState.State.playing))
        Core.app.post(::trySetRule)
}


fun showInfo(player: Player) {
    player.sendMessage(
        """
        [violet]当前地图为[gold]塔防模式
        [magenta]===规则介绍===
        [violet]1.怪物只会攻击核心,可以放心建筑
        2.过于靠近怪物的兵可能会被杀掉
        3.道路上不准建筑(原因由上)
        4.核心附加的道路允许放置武装传送带
        5.怪物会掉落大量战利品(不限距离)
        6.部分物品被禁用(保证平衡)
    """.trimIndent(), MsgType.InfoMessage
    )
}
listen<EventType.PlayEvent> {
    if (tdMode) Core.app.post {
        Groups.player.forEach(::showInfo)
    }
}
listen<EventType.PlayerJoin> {
    if (tdMode) showInfo(it.player)
}