@file:Depends("coreMindustry/utilNext", "调用菜单")
@file:Depends("coreMindustry/utilMapRule", "修改核心单位,单位属性")

package mapScript

import arc.util.Align
import arc.util.Time
import coreLibrary.lib.util.loop
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.entities.Units
import mindustry.game.Team
import mindustry.gen.Iconc
import mindustry.type.StatusEffect
import mindustry.type.UnitType
import mindustry.world.blocks.storage.CoreBlock
import kotlin.math.floor
import kotlin.random.Random

/**@author xkldklp
 * https://mdt.wayzer.top/v2/map/13599/latest
 * 代码优化整理: WayZer
 */
name = "[sky]STAR[red]BLAST"

val menu = contextScript<coreMindustry.UtilNext>()

fun Player.upgrade(type: UnitType, cost: Int, coreCost: Float): mindustry.gen.Unit {
    val unit = unit()
    if (unit.stack().amount >= cost) {
        unit(type.spawn(team(), unit).apply {
            spawnedByCore = true
            if (type != UnitTypes.horizon) {//轰炸机太慢了不能攻击
                apply(StatusEffects.electrified, 999999f)
                apply(StatusEffects.sapped, 999999f)
                apply(StatusEffects.freezing, 999999f)
            }
            if (type == UnitTypes.mega) {//你跑太快力
                apply(StatusEffects.sporeSlowed, 999999f)
            }
            apply(StatusEffects.unmoving, 5f * 60f)
            apply(StatusEffects.disarmed, 10f * 60f)
            apply(StatusEffects.invincible, 5f * 60f)
        })
        core().items.remove(Items.copper, (core().items.get(Items.copper) * coreCost).toInt())
    }
    return unit()
}

fun Player.teamMessage(message: String) {
    Call.label("${name}:[#${team().color}]${message}", 12f, x, y)
    //大  声  密  谋
}

fun mindustry.gen.Unit.addItem(amount: Int) {
    val added = amount.coerceAtMost(itemCapacity() - stack.amount)
    stack.amount += added
    if (added < amount)//剩下的给予核心
        core()?.items?.add(Items.copper, amount - added)
}

fun itemDrop(x: Float, y: Float, unit: Any, amount: Int) {
    val units = buildList {
        Units.nearby(null, x, y, 20 * 8f) {
            if (it != unit) add(it)
        }
    }
    if (units.isEmpty()) return
    if (units.size < amount)
        units.forEach { it.addItem(amount / units.size) }
    //零散数随机分配
    units.shuffled().take(amount % units.size)
        .forEach { it.addItem(1) }
}

fun Player.statusEffect(effect: StatusEffect, cost: Int) {
    val unit = unit()
    if (unit.stack().amount >= cost) {
        if (unit.hasEffect(effect)) {
            unit.unapply(effect)
        } else {
            unit.apply(effect, 999999f)
        }
        unit.stack().amount -= cost
    }
}

suspend fun Player.sendMenu() {
    menu.sendMenuBuilder<Unit>(
        this, 30_000, "选择升级",
        """
            [green]升级单位使用单位背包资源
            [red]有些单位升级需要使用核心资源！(百分比)
            [cyan]T4需要核心铜>=3000
            [cyan]T5需要核心铜>=18000
        """.trimIndent()
    ) {
        fun unitUpgrade(type: UnitType, icon: Char, cost: Int, coreCost: Float = 0f) =
            "$icon$cost + $coreCost%(${(coreCost * core().items.get(Items.copper)).toInt()})核心资源" to suspend {
                upgrade(type, cost, coreCost);Unit
            }

        fun effect(effect: StatusEffect, icon: Char, cost: Int) =
            "移除/添加 $icon$cost" to suspend { statusEffect(effect, cost) }

        when (unit().type) {//玩家单位
            UnitTypes.flare -> this += listOf(
                unitUpgrade(UnitTypes.horizon, Iconc.unitHorizon, 0),
                unitUpgrade(UnitTypes.poly, Iconc.unitPoly, 0),
            )
            UnitTypes.horizon -> this += listOf(
                unitUpgrade(UnitTypes.zenith, Iconc.unitZenith, 30),
                unitUpgrade(UnitTypes.mega, Iconc.unitMega, 30, 0.02f),
            )
            UnitTypes.poly -> this += listOf(
                unitUpgrade(UnitTypes.mega, Iconc.unitMega, 30),
                unitUpgrade(UnitTypes.cyerce, Iconc.unitCyerce, 30, 0.02f),
            )
        }
        if (core().items.get(Items.copper) >= 3000) {
            when (unit().type) {
                UnitTypes.zenith -> this += listOf(
                    unitUpgrade(UnitTypes.antumbra, Iconc.unitAntumbra, 80, 0.05f),
                    unitUpgrade(UnitTypes.quad, Iconc.unitQuad, 80, 0.1f),
                )
                UnitTypes.mega -> this += listOf(
                    unitUpgrade(UnitTypes.quad, Iconc.unitQuad, 60, 0.05f),
                    unitUpgrade(UnitTypes.aegires, Iconc.unitAegires, 60, 0.1f),
                )
                UnitTypes.cyerce -> this += listOf(
                    unitUpgrade(UnitTypes.aegires, Iconc.unitAegires, 80, 0.05f),
                    unitUpgrade(UnitTypes.sei, Iconc.unitSei, 80, 0.15f),
                )
            }
        }
        if (core().items.get(Items.copper) >= 18000) {
            when (unit().type) {
                UnitTypes.antumbra -> this += listOf(
                    unitUpgrade(UnitTypes.eclipse, Iconc.unitEclipse, 180, 0.2f),
                    unitUpgrade(UnitTypes.oct, Iconc.unitOct, 180, 0.5f),
                    unitUpgrade(UnitTypes.navanax, Iconc.unitNavanax, 180, 0.5f),
                    unitUpgrade(UnitTypes.omura, Iconc.unitOmura, 180, 0.5f),
                )
                UnitTypes.quad -> this += listOf(
                    unitUpgrade(UnitTypes.eclipse, Iconc.unitEclipse, 140, 0.5f),
                    unitUpgrade(UnitTypes.oct, Iconc.unitOct, 140, 0.2f),
                    unitUpgrade(UnitTypes.navanax, Iconc.unitNavanax, 140, 0.5f),
                    unitUpgrade(UnitTypes.omura, Iconc.unitOmura, 140, 0.5f),
                )
                UnitTypes.aegires -> this += listOf(
                    unitUpgrade(UnitTypes.eclipse, Iconc.unitEclipse, 170, 0.5f),
                    unitUpgrade(UnitTypes.oct, Iconc.unitOct, 170, 0.5f),
                    unitUpgrade(UnitTypes.navanax, Iconc.unitNavanax, 170, 0.2f),
                    unitUpgrade(UnitTypes.omura, Iconc.unitOmura, 170, 0.5f),
                )
                UnitTypes.sei -> this += listOf(
                    unitUpgrade(UnitTypes.eclipse, Iconc.unitEclipse, 150, 0.5f),
                    unitUpgrade(UnitTypes.oct, Iconc.unitOct, 150, 0.5f),
                    unitUpgrade(UnitTypes.navanax, Iconc.unitNavanax, 150, 0.5f),
                    unitUpgrade(UnitTypes.omura, Iconc.unitOmura, 150, 0.2f),
                )
            }
        }
        if (unit().hasEffect(StatusEffects.electrified)) {
            this += listOf(
                effect(
                    StatusEffects.electrified,
                    Iconc.statusElectrified,
                    (unit().itemCapacity() * 0.5f).toInt()
                )
            )
        }
        if (unit().hasEffect(StatusEffects.sapped)) {
            this += listOf(effect(StatusEffects.sapped, Iconc.statusSapped, (unit().itemCapacity() * 0.5f).toInt()))
        }
        if (unit().hasEffect(StatusEffects.freezing)) {
            this += listOf(effect(StatusEffects.freezing, Iconc.statusFreezing, (unit().itemCapacity() * 0.5f).toInt()))
        }
        if (!unit().hasEffect(StatusEffects.overclock)) {
            this += listOf(
                effect(
                    StatusEffects.overclock,
                    Iconc.statusOverclock,
                    (unit().itemCapacity() * 0.6f).toInt()
                )
            )
        }
        if (!unit().hasEffect(StatusEffects.overdrive)) {
            this += listOf(effect(StatusEffects.overdrive, Iconc.statusOverdrive, (unit().itemCapacity() * 1f).toInt()))
        }
        if (!unit().hasEffect(StatusEffects.boss)) {
            this += listOf(effect(StatusEffects.boss, Iconc.statusBoss, (unit().itemCapacity() * 1f).toInt()))
        }
        add(buildList {
            add("团队指令：发起进攻" to {
                teamMessage("\uE861进攻\uE861")
            })
            add("团队指令：注意基地" to {
                teamMessage("⚠基地\uE84D")
            })
            add("团队指令：跟着我" to {
                teamMessage("\uF844请求跟随\uE872")
            })
        })
        add(buildList {
            add("团队指令：No" to {
                teamMessage("\uE815NO\uE815")
            })
            add("团队指令：OK" to {
                teamMessage("\uE800OK\uE800")
            })
        })
        add(listOf("取消" to {}))
    } ?: sendMessage("[red]选择超时")
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
        registerMapRule((Blocks.coreShard as CoreBlock)::unitType) { UnitTypes.flare }
        registerMapRule((Blocks.coreFoundation as CoreBlock)::unitType) { UnitTypes.flare }
        registerMapRule((Blocks.coreNucleus as CoreBlock)::unitType) { UnitTypes.flare }
        registerMapRule(UnitTypes.flare::health) { 150f }
        registerMapRule(UnitTypes.cyerce::flying) { true }//t3海辅
        registerMapRule(UnitTypes.cyerce::health) { 400f }
        registerMapRule(UnitTypes.cyerce::armor) { 0f }
        registerMapRule(UnitTypes.cyerce.weapons.get(2).bullet::healPercent) { 0f }
        registerMapRule(UnitTypes.cyerce.weapons.get(2).bullet.fragBullet::healPercent) { 0f }
        registerMapRule(UnitTypes.sei::flying) { true }//t4海战
        registerMapRule(UnitTypes.sei::health) { 1800f }
        registerMapRule(UnitTypes.sei.weapons.get(0).bullet::damage) { 15f }
        registerMapRule(UnitTypes.sei.weapons.get(0).bullet::splashDamage) { 7f }
        registerMapRule(UnitTypes.sei.weapons.get(1).bullet::damage) { 24f }
        registerMapRule(UnitTypes.sei::armor) { 0f }
        registerMapRule(UnitTypes.aegires::flying) { true }//t4海辅
        registerMapRule(UnitTypes.aegires::health) { 1200f }
        registerMapRule(UnitTypes.aegires::armor) { 0f }
        registerMapRule(UnitTypes.omura::flying) { true }//t5海战
        registerMapRule(UnitTypes.omura::health) { 5600f }
        registerMapRule(UnitTypes.omura::armor) { 0f }
        registerMapRule(UnitTypes.omura.weapons.get(0).bullet::damage) { 600f }
        registerMapRule(UnitTypes.omura.weapons.get(0).bullet::status) { StatusEffects.shielded }
        registerMapRule(UnitTypes.omura.weapons.get(0).bullet::statusDuration) { 60f * 5f }
        registerMapRule(UnitTypes.navanax::flying) { true }//t5海辅
        registerMapRule(UnitTypes.navanax::health) { 4600f }
        registerMapRule(UnitTypes.navanax::armor) { -4f }
        registerMapRule(UnitTypes.navanax.weapons.get(4).bullet::status) { StatusEffects.disarmed }
        registerMapRule(UnitTypes.navanax.weapons.get(4).bullet::statusDuration) { 0.3f * 60 }
        registerMapRule(UnitTypes.poly.weapons.get(0).bullet::healPercent) { 0f }
        registerMapRule(UnitTypes.poly.weapons.get(0).bullet::damage) { 34f }
        registerMapRule(UnitTypes.poly::armor) { 0f }
        registerMapRule(UnitTypes.mega::health) { 560f }
        registerMapRule(UnitTypes.mega::armor) { 0f }
        registerMapRule(UnitTypes.mega.weapons.get(0).bullet::healPercent) { 0f }
        registerMapRule(UnitTypes.mega.weapons.get(0).bullet::damage) { 16f }
        registerMapRule(UnitTypes.mega.weapons.get(2).bullet::healPercent) { 0f }
        registerMapRule(UnitTypes.mega.weapons.get(2).bullet::damage) { 8f }
        registerMapRule(UnitTypes.zenith::armor) { 0f }
        registerMapRule(UnitTypes.antumbra::health) { 3600f }
        registerMapRule(UnitTypes.antumbra::armor) { 0f }
        registerMapRule(UnitTypes.antumbra.weapons.get(0).bullet::damage) { 9f }
        registerMapRule(UnitTypes.antumbra.weapons.get(0).bullet::splashDamage) { 18f }
        registerMapRule(UnitTypes.antumbra.weapons.get(5).bullet::damage) { 33f }
        registerMapRule(UnitTypes.quad::health) { 3600f }
        registerMapRule(UnitTypes.quad::armor) { 0f }
        registerMapRule(UnitTypes.quad.weapons.get(0).bullet::splashDamage) { 110f }
        registerMapRule(UnitTypes.quad.weapons.get(0).bullet::collidesAir) { true }
        registerMapRule(UnitTypes.eclipse::health) { 7200f }
        registerMapRule(UnitTypes.eclipse::armor) { 0f }
        registerMapRule(UnitTypes.eclipse.weapons.get(0).bullet::damage) { 55f }
        registerMapRule(UnitTypes.eclipse.weapons.get(2).bullet::splashDamage) { 25f }
        registerMapRule(UnitTypes.oct::health) { 8000f }
        registerMapRule(UnitTypes.oct::armor) { -50f }
    }
    launch(Dispatchers.gamePost) {
        state.teams.getActive().each { data ->
            data.core()?.run { health += 20000f }
        }
    }
    loop(Dispatchers.game) {
        delay(500)
        state.teams.getActive().each { data ->
            val core = data.core() ?: return@each
            if (core.health < core.maxHealth * 0.5f && core.items.get(Items.copper) >= 320) {
                core.items.remove(Items.copper, 320)
                core.health += core.maxHealth * 0.5f
                itemDrop(core.x, core.y, "This is a build", 320)
            }
            data.units.each {//背包20%的铜修复血的10%
                var itemDropAmount = 0
                while (it.health < it.maxHealth * 0.9f && it.stack.amount >= (it.itemCapacity() * 0.2f).toInt()) {
                    it.health += it.maxHealth * 0.1f
                    it.stack.amount -= (it.itemCapacity() * 0.2f).toInt()
                    itemDropAmount += (it.itemCapacity() * 0.2f).toInt()
                }
                if (itemDropAmount >= 1) itemDrop(it.x, it.y, it, (itemDropAmount.toFloat() * 0.8f).toInt())
            }
        }
    }
    //刷新区域半边长
    val range = state.rules.tags.getInt("refreshRange", 50)
    loop(Dispatchers.game) {
        delay(1000)
        repeat(3) {
            val tile = world.tiles.getn(
                Random.nextInt(world.width() / 2 - range, world.width() / 2 + range),
                Random.nextInt(world.height() / 2 - range, world.height() / 2 + range)
            )
            tile.setNet(Blocks.thoriumWall, Team.crux, 0)
        }
    }
    loop(Dispatchers.game) {
        delay(5000)
        val text = "各队伍核心情况\n" + state.teams.getActive().joinToString("\n") {
            val core = it.core()!!
            "[#${core.team.color}]${core.team.name}[white]" + "\uF838 ${core.items.get(Items.copper)}" +
                    "\uF867${core.health}/${core.maxHealth}" +
                    "(${(floor(core.items.get(Items.copper) / 320f) * core.maxHealth * 0.5f)})"
        }
        Call.infoPopup(
            text, 5.013f,
            Align.topLeft, 350, 0, 0, 0
        )
    }
}

val oreCostIncreaseTime = state.rules.tags.getInt("oreCostIncreaseTime", 240) * 1000f
val startTime by lazy { Time.millis() }
listen<EventType.BlockDestroyEvent> { t ->
    val time = (Time.timeSinceMillis(startTime) / oreCostIncreaseTime).toInt()
    val amount = Random.nextInt(time + 1, time + 3)
    itemDrop(t.tile.worldx(), t.tile.worldy(), "This is a build", amount)
}

listen<EventType.UnitDestroyEvent> { u ->
    itemDrop(u.unit.x, u.unit.y, u.unit, u.unit.stack.amount + (u.unit.itemCapacity() * 0.4f).toInt())
}