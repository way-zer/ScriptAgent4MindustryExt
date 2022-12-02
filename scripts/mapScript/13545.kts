@file:Depends("coreMindustry/utilNext", "调用菜单") @file:Depends("coreMindustry/utilMapRule", "修改核心单位,单位属性")

package mapScript

import arc.math.geom.Geometry
import arc.util.Align
import arc.util.Tmp
import coreLibrary.lib.util.loop
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.core.World
import mindustry.entities.Units
//import mindustry.entities.units.UnitCommand
import mindustry.game.Rules.TeamRule
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Iconc
import mindustry.gen.WaterMovec
import mindustry.type.Item
import mindustry.type.ItemStack
import mindustry.type.UnitType
import mindustry.world.Tile
import mindustry.world.blocks.storage.CoreBlock.CoreBuild
import kotlin.reflect.KMutableProperty1

/**@author miner
 */
name = "CoreWar"

val menu = contextScript<coreMindustry.UtilNext>()

val spawnRadius = 5
val cycleItems: Map<Item, Float> = mapOf(
    Items.lead to 1f, Items.sand to 0.5f, Items.coal to 1f, Items.scrap to 0.5f
)

val teamUnitCounter: Map<Team, TeamData> by lazy { state.teams.getActive().map { it.team to TeamData() }.toMap() }

val playerSpawnCore: MutableMap<Player, CoreBuild?> = mutableMapOf()

val Team.data: TeamData
    get() = teamUnitCounter[this] ?: TeamData()

var Player.spawnCore: CoreBuild?
    get() = playerSpawnCore[this]
    set(core) {
        playerSpawnCore[this] = core
    }

fun getTechCost(cost: Int, count: Int): Int {
    return (count + 1) * cost + (count / 4f * cost).toInt()
}

fun getUnitCost(cost: Int, count: Int): Int = ((1 + count / 6f) * cost).toInt()

fun Team.upgradeTech(cost: Int, field: KMutableProperty1<TeamRule, Float>): Boolean {
    val core = core()

    if (core.items.get(Items.copper) >= cost) {
        val rule = rules()

        field.apply { set(rule, get(rule) + 0.05f) }

        data.addTechCount(field)

        core.items.remove(Items.copper, cost)

        return true
    }

    return false
}

fun spawnUnit(core: CoreBuild?, type: UnitType, statusDur: Float, cost: Int): Boolean {
    if (core == null) return false

    if (core.items.get(Items.copper) < cost) return false

    val team = core.team

    if (!Units.canCreate(team, type)) return false

    val unit = type.create(team)

    if (unit is WaterMovec) {
        val found: MutableList<Tile> = mutableListOf()
        Geometry.circle(core.tileX(), core.tileY(), spawnRadius) { tx, ty ->
            val tile = world.tile(tx, ty)
            if (tile != null && tile.floor().isLiquid && unit.canPass(tx, ty)) {
                found += tile
            }
        }

        if (found.size == 0) return false

        Tmp.v2.set(found.random())

        unit.set(Tmp.v2.x, Tmp.v2.y)
    } else {
        var times = 0
        while (true) {
            Tmp.v1.rnd(spawnRadius.toFloat() * tilesize)

            val sx = core.x + Tmp.v1.x
            val sy = core.y + Tmp.v1.y

            if (unit.canPass(World.toTile(sx), World.toTile(sy))) {
                unit.set(sx, sy)
                break
            }

            if (++times > 20) {
                return false
            }
        }
    }

    unit.apply {
        apply(StatusEffects.electrified, statusDur * 60)
        add()
    }

    core.items.remove(Items.copper, cost)

    return true
}

fun Team.cycleItem() = core()?.run {
    cycleItems.forEach { (item, weight) ->
        run {
            val amount = items.get(item)
            items.remove(item, amount)
            items.add(Items.copper, (amount * weight).toInt())
        }
    }
}

fun Float.format(i: Int = 2): String {
    return "%.${i}f".format(this)
}

fun Player.teamMessage(message: String) {
    Groups.player.filter { it.team() == team() }.forEach { Call.infoToast(it.con, message, 4f) }
}

suspend fun Player.sendMenu() {
    menu.sendMenuBuilder<Unit>(
        this, 30_000, "购买菜单", """
            [yellow]可用资源: ${Iconc.itemCopper}${core().items.get(Items.copper)}
            [green]点击按钮购买对应队伍科技和单位
        """.trimIndent()
    ) {
        fun spawnUnit(
            type: UnitType,
            icon: Char,
            statusDur: Float = 0f,
            defaultCost: Int,
            costGetter: (spawnCount: Int) -> Int = { getUnitCost(defaultCost, it) }
        ) = "$icon\n" + statusDur.run {
            if (this == 0f) ""
            else StatusEffects.electrified.emoji() + statusDur.format(1) + "s" + "\n"
        } + "${Iconc.itemCopper}${costGetter(team().data().countType(type))}" to suspend {
            val cost = costGetter(team().data().countType(type))

            val announce = spawnUnit(spawnCore, type, statusDur, cost)

            if (announce) {
                teamMessage(
                    "[#${team().color}]${team().name}[white]" +
                    "${coloredName()}[white]花费${Iconc.itemCopper}${cost}" +
                    "购买了单位${type.emoji()}"
                )
            }

            sendMenu()
        }

        fun techUpgrade(describe: String, defCost: Int, field: KMutableProperty1<TeamRule, Float>) = """
                升级${describe}
                (${field.run { get(team().rules()).let { "${it.format()} -> ${(it + 0.05f).format()}" } }})
                ${Iconc.itemCopper}${getTechCost(defCost, team().data.getTechCount(field))}
            """.trimIndent() to suspend {
            val cost = getTechCost(defCost, team().data.getTechCount(field))

            val announce = team().upgradeTech(cost, field)

            if (announce) {
                teamMessage(
                    "[#${team().color}]${team().name}[white]${coloredName()}[white]" +
                    "花费${Iconc.itemCopper}${cost}" +
                    "购买了属性${describe}(${field.run { get(team().rules()).let { "${it.format()} -> ${(it + 0.05f).format()}" } }})"
                )
            }
            sendMenu()
        }

        this += listOf(
            techUpgrade("建筑攻击", 100, TeamRule::blockDamageMultiplier), techUpgrade("建筑血量", 300, TeamRule::blockHealthMultiplier)
        )

        this += listOf(
            techUpgrade("单位攻击", 100, TeamRule::unitDamageMultiplier)
            // TODO: 单位血量
        )

        this += listOf(spawnUnit(UnitTypes.mono, Iconc.unitMono, defaultCost = 200) { (it + 1) * 200 }, "${cycleItems.keys.joinToString("") { it.emoji() }} -> ${Iconc.itemCopper}" to {
            team().cycleItem()
            sendMenu()
        })

        this += listOf(
            spawnUnit(UnitTypes.crawler, Iconc.unitCrawler, 1f, 42),
            spawnUnit(UnitTypes.flare, Iconc.unitFlare, 1f, 55),
            spawnUnit(UnitTypes.risso, Iconc.unitRisso, 1f, 71),
            spawnUnit(UnitTypes.retusa, Iconc.unitRetusa, 1f, 71),
        )

        this += listOf(
            spawnUnit(UnitTypes.mace, Iconc.unitMace, 2f, 120),
            spawnUnit(UnitTypes.atrax, Iconc.unitAtrax, 2f, 125),
            spawnUnit(UnitTypes.minke, Iconc.unitMinke, 2f, 140),
            spawnUnit(UnitTypes.horizon, Iconc.unitHorizon, 2f, 120),
        )

        this += listOf(
            spawnUnit(UnitTypes.fortress, Iconc.unitFortress, 8f, 450),
            spawnUnit(UnitTypes.bryde, Iconc.unitBryde, 12f, 700),
            spawnUnit(UnitTypes.zenith, Iconc.unitZenith, 9f, 500),
        )

        this += listOf(
            spawnUnit(UnitTypes.scepter, Iconc.unitScepter, 83f, 5_000),
            spawnUnit(UnitTypes.antumbra, Iconc.unitAntumbra, 100f, 5_000),
            spawnUnit(UnitTypes.sei, Iconc.unitSei, 133f, 6_000),
        )

        this += listOf(
            spawnUnit(UnitTypes.vela, Iconc.unitVela, 300f, 18_000),
            spawnUnit(UnitTypes.arkyid, Iconc.unitArkyid, 200f, 12_000),
        )

        this += listOf(
            spawnUnit(UnitTypes.reign, Iconc.unitRisso, 300f, 55_000),
            spawnUnit(UnitTypes.corvus, Iconc.unitCorvus, 300f, 77_000),
        )

        this += listOf(
            spawnUnit(UnitTypes.eclipse, Iconc.unitEclipse, 300f, 58_000),
            spawnUnit(UnitTypes.omura, Iconc.unitOmura, 300f, 75_000),
        )

        add(listOf("取消" to { }))
    }
}

listen<EventType.TapEvent> {
    if (it.tile.build is CoreBuild) {
        val clickedCore = it.tile.build as CoreBuild

        val player = it.player
        if (!player.dead() && it.tile.team() == player.team()) {
            launch(Dispatchers.game) {
                player.spawnCore = clickedCore
                player.sendMenu()
            }
        }
    }
}

onEnable {
    contextScript<coreMindustry.UtilMapRule>().apply {
        //registerMapRule(Blocks.commandCenter::requirements) { ItemStack.with(Items.copper, 150) }
        registerMapRule(Blocks.coreShard::itemCapacity) { Int.MAX_VALUE }
        registerMapRule(Blocks.coreFoundation::itemCapacity) { Int.MAX_VALUE }
        registerMapRule(Blocks.coreFoundation::itemCapacity) { Int.MAX_VALUE }
    }

    loop(Dispatchers.game) {
        delay(5000)
        val text = "[green]点击核心打开购买菜单[white]\n" + "[yellow]指挥中心造价为100${Iconc.itemCopper}(136已移除，请忽略)\n" + state.teams.getActive().filter { it.hasCore() }.joinToString("\n") {
            val rules = it.team.rules()
            "[#${it.team.color}]${it.team.name}[white]属性" + "${Iconc.blockDuo}: " + "${Iconc.defense}${rules.blockHealthMultiplier.format()}" + "${Iconc.commandAttack}${rules.blockDamageMultiplier.format()}" + "   " + "${Iconc.unitGamma}: " + "${Iconc.commandAttack}${rules.unitDamageMultiplier.format()}"
        }
        Call.infoPopup(
            text, 5.013f, Align.topLeft, 350, 0, 0, 0
        )
    }

    //state.teams.getActive().forEach { it.command = UnitCommand.idle }

}

class TeamData {
    private val techCounter: Counter<KMutableProperty1<TeamRule, Float>> = Counter()

    fun addTechCount(field: KMutableProperty1<TeamRule, Float>): Unit = techCounter.add(field)

    fun getTechCount(field: KMutableProperty1<TeamRule, Float>): Int = techCounter.get(field)
}

class Counter<K> {
    private val counter: MutableMap<K, Int> = mutableMapOf()

    fun add(key: K) {
        counter[key] = counter.getOrDefault(key, 0) + 1
    }

    fun get(key: K) = counter.getOrDefault(key, 0)
}