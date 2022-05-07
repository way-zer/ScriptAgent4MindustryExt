@file:Depends("coreMindustry/utilNext", "调用菜单")
@file:Depends("coreMindustry/utilMapRule", "修改核心单位")

package mapScript

import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.UnitTypes
import mindustry.type.Item
import mindustry.world.blocks.storage.CoreBlock

val menu = contextScript<coreMindustry.UtilNext>()

/**@author xkldklp
 * https://mdt.wayzer.top/v2/map/13679/latest
 */
name = "随机塔防"

val turrets = arrayOf(
    Blocks.duo,
    Blocks.duo,
    Blocks.duo,
    Blocks.duo,
    Blocks.salvo,
    Blocks.salvo,
    Blocks.salvo,
    Blocks.spectre,
    Blocks.arc,
    Blocks.arc,
    Blocks.arc,
    Blocks.arc,
    Blocks.lancer,
    Blocks.lancer,
    Blocks.lancer,
    Blocks.meltdown,
    Blocks.wave,//useless thing(
    Blocks.wave,
    Blocks.tsunami,
    Blocks.hail,
    Blocks.hail,
    Blocks.hail,
    Blocks.hail,
    Blocks.hail,
    Blocks.hail,
    Blocks.ripple,
    Blocks.ripple,
    Blocks.ripple,
    Blocks.scorch,
    Blocks.scorch,
    Blocks.scorch,
    Blocks.fuse,
    Blocks.fuse,
    Blocks.swarmer,
    Blocks.cyclone,
    Blocks.foreshadow
)
val rareTurrets = setOf(
    Blocks.spectre,
    Blocks.meltdown,
    Blocks.foreshadow
)
listen<EventType.BlockBuildEndEvent> {
    if (it.tile.block() != Blocks.duo) return@listen
    var turret = turrets.random()
    if (turret in rareTurrets)
        turret = turrets.random()//概率为：1225/1
    val p = it.unit.player ?: null
    launch(Dispatchers.gamePost) {
        it.tile.setNet(turret, it.tile.team(), 0)
        val msg = buildString {
            appendLine(p?.name ?: "UNKNOWN")
            appendLine("[yellow]抽取到了[cyan]${turret.emoji()}${turret.localizedName}")
            if (turret in rareTurrets)
                appendLine("[yellow]超稀有炮台！")
        }
        Call.label(msg, turret.health / 500f, it.tile.worldx(), it.tile.worldy())
    }
}

suspend fun Player.sendMenu() {
    menu.sendMenuBuilder<Unit>(
        this, 30_000, "资源商店",
        """
            [green]花费铜购买资源
            [cyan]cheat模式 一次弹药终生射击
        """.trimIndent()
    ) {
        fun buy(type: Item, cost: Int) =
            "${type.emoji()}\n[cyan]$cost" to suspend {
                val core = team().core()
                if (core.items.get(Items.copper) >= cost) {
                    core().items.remove(Items.copper, cost)
                    unit().addItem(type, 1)
                }
            }
        this += listOf(
            buy(Items.coal, 100),
            buy(Items.graphite, 100),
            buy(Items.metaglass, 100),
            buy(Items.titanium, 100),
            buy(Items.silicon, 100),
        )
        this += listOf(
            buy(Items.pyratite, 500),
            buy(Items.plastanium, 500),
            buy(Items.thorium, 500),
        )
        this += listOf(
            buy(Items.surgeAlloy, 4000),
            buy(Items.blastCompound, 1000),
        )
        add(listOf("取消" to {}))
    }
}

listen<EventType.TapEvent> {
    if (it.tile.block() is CoreBlock) {
        val player = it.player
        if (!player.dead() && it.tile.team() == player.team()) {
            launch(Dispatchers.game) { player.sendMenu() }
        }
    }
}

onEnable {
    contextScript<coreMindustry.UtilMapRule>().apply {
        registerMapRule(UnitTypes.gamma.weapons.get(0).bullet::damage) { 0f }
        registerMapRule(UnitTypes.gamma::health) { 1f }
    }
}


