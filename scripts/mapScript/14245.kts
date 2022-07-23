@file:Depends("coreMindustry/utilNext", "调用菜单") 
@file:Depends("coreMindustry/utilMapRule", "修改核心单位,单位属性")

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
import mindustry.world.blocks.production.Drill

import java.lang.Math.pow

/**@author xem8k5 miner
 */
name = "PlanetWar"

val menu = contextScript<coreMindustry.UtilNext>()

val spawnRadius = 5
val cycleItems: Map<Item, Float> = mapOf(
    Items.tungsten to 2f, Items.carbide to 1f, Items.graphite to 1f, Items.thorium to 3f
)

val teamUnitCounter: Map<Team, TeamData> by lazy { state.teams.getActive().map { it.team to TeamData() }.toMap() }

val playerSpawnCore: MutableMap<Player, CoreBuild?> = mutableMapOf()
val playerDrill: MutableMap<Player, Drill?> = mutableMapOf()

val Team.data: TeamData
    get() = teamUnitCounter[this] ?: TeamData()


var Player.spawnCore: CoreBuild?
    get() = playerSpawnCore[this]
    set(core) {
        playerSpawnCore[this] = core
    }
    
var Player.drill: Drill?
    get() = playerDrill[this]
    set(drill) {
        playerDrill[this] = drill
    }

fun getTechCost(cost: Int, count: Int): Int {
    return (count + 1) * cost + (count / 4f * cost).toInt()
}

fun getUnitCost(cost: Int, count: Int): Int = (Math.pow(1.1,count.toDouble()) * cost).toInt()

fun Team.upgradeTech(cost: Int, field: KMutableProperty1<TeamRule, Float>): Boolean {
    val core = core()

    if (core.items.get(Items.beryllium) >= cost) {
        val rule = rules()

        field.apply { set(rule, get(rule) + 0.05f) }

        data.addTechCount(field)

        core.items.remove(Items.beryllium, cost)

        return true
    }

    return false
}

fun spawnUnit(core: CoreBuild?, type: UnitType, statusDur: Float, cost: Int): Boolean {
    if (core == null) return false

    if (core.items.get(Items.beryllium) < cost) return false

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

    core.items.remove(Items.beryllium, cost)

    return true
}

fun Team.cycleItem() = core()?.run {
    cycleItems.forEach { (item, weight) ->
        run {
            val amount = items.get(item)
            items.remove(item, amount)
            items.add(Items.beryllium, (amount * weight).toInt())
        }
    }
}

fun Team.turnItem() = core()?.run {
    if(items.get(Items.copper) <= 20){
            
    }else{
        items.add(Items.tungsten, 1)
        items.add(Items.carbide, 1)
        items.add(Items.graphite, 1)
        items.add(Items.thorium, 1)
        items.remove(Items.copper, 20)            
    }
}


fun Team.exchangeItem() = core()?.run {
    if(items.get(Items.copper) <= 500){
    
    }else{
        items.add(Items.beryllium, 500)
        items.remove(Items.copper, 1000)    
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
            [yellow]可用资源: ${Iconc.itemBeryllium}${core().items.get(Items.beryllium)}
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
        } + "${Iconc.itemBeryllium}${costGetter(team().data().countType(type))}" to suspend {
            val cost = costGetter(team().data().countType(type))

            val announce = spawnUnit(spawnCore, type, statusDur, cost)

            if (announce) {
                teamMessage(
                    "[#${team().color}]${team().name}[white]" +
                    "${coloredName()}[white]花费${Iconc.itemBeryllium}${cost}" +
                    "购买了单位${type.emoji()}"
                )
            }

            sendMenu()
        }

        fun techUpgrade(describe: String, defCost: Int, field: KMutableProperty1<TeamRule, Float>) = """
                升级${describe}
                (${field.run { get(team().rules()).let { "${it.format()} -> ${(it + 0.05f).format()}" } }})
                ${Iconc.itemBeryllium}${getTechCost(defCost, team().data.getTechCount(field))}
            """.trimIndent() to suspend {
            val cost = getTechCost(defCost, team().data.getTechCount(field))

            val announce = team().upgradeTech(cost, field)

            if (announce) {
                teamMessage(
                    "[#${team().color}]${team().name}[white]${coloredName()}[white]" +
                    "花费${Iconc.itemBeryllium}${cost}" +
                    "购买了属性${describe}(${field.run { get(team().rules()).let { "${it.format()} -> ${(it + 0.05f).format()}" } }})"
                )
            }
            sendMenu()
        }

        this += listOf(
            techUpgrade("建筑攻击", 100, TeamRule::blockDamageMultiplier), techUpgrade("建筑血量", 300, TeamRule::blockHealthMultiplier)
        )

        this += listOf(
            techUpgrade("单位攻击", 100, TeamRule::unitDamageMultiplier),
            spawnUnit(UnitTypes.mono, Iconc.unitMono, defaultCost = 200) { (it + 1) * 50 + 200 },
        )
        
        
        this += listOf(
            spawnUnit(UnitTypes.stell, Iconc.unitStell, 1f, 100),
            spawnUnit(UnitTypes.merui, Iconc.unitMerui, 1f, 125),
            spawnUnit(UnitTypes.elude, Iconc.unitElude, 1f, 145),
        )

        this += listOf(
            spawnUnit(UnitTypes.locus, Iconc.unitLocus, 2f, 380),
            spawnUnit(UnitTypes.cleroi, Iconc.unitCleroi, 2f, 440),
            spawnUnit(UnitTypes.avert, Iconc.unitAvert, 2f, 530),
        )

        this += listOf(
            spawnUnit(UnitTypes.precept, Iconc.unitPrecept, 8f, 1200),
            spawnUnit(UnitTypes.anthicus, Iconc.unitAnthicus, 12f, 2000),
            spawnUnit(UnitTypes.obviate, Iconc.unitObviate, 9f, 2500),
        )

        this += listOf(
            spawnUnit(UnitTypes.vanquish, Iconc.unitVanquish, 83f, 5_000),
            spawnUnit(UnitTypes.tecta, Iconc.unitTecta, 100f, 7_900),
            spawnUnit(UnitTypes.quell, Iconc.unitQuell, 133f, 8_800),
        )

        this += listOf(
            spawnUnit(UnitTypes.conquer, Iconc.unitConquer, 300f, 14_600),
            spawnUnit(UnitTypes.collaris, Iconc.unitCollaris, 200f, 22_600),
            spawnUnit(UnitTypes.disrupt, Iconc.unitDisrupt, 200f, 18_400),
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
    
    if (it.tile.block() is Drill) {
        val player = it.player
        if (!player.dead() && it.tile.team() == player.team()) {
            val items = it.tile.build.items
            if(items != null){
                player.unit().core()?.items?.add(Items.copper, items.get(Items.copper).toInt())
                it.tile.build.items?.remove(Items.copper, items.get(Items.copper).toInt())
                
                player.unit().core()?.items?.add(Items.beryllium, items.get(Items.beryllium).toInt())
                it.tile.build.items?.remove(Items.beryllium, items.get(Items.beryllium).toInt())
            }
        }
    }
}

onEnable {


    contextScript<coreMindustry.UtilMapRule>().apply {
        registerMapRule(Blocks.coreShard::itemCapacity) { 1_000_000 }
        registerMapRule(Blocks.coreFoundation::itemCapacity) { 1_000_000 }
        registerMapRule(Blocks.coreNucleus::itemCapacity) { 1_000_000 }
        
        registerMapRule(Blocks.coreBastion::itemCapacity) { 1_000_000 }
        registerMapRule(Blocks.coreCitadel::itemCapacity) { 1_000_000 }
        registerMapRule(Blocks.coreAcropolis::itemCapacity) { 1_000_000 }
    }
    

    loop(Dispatchers.game) {
        delay(5000)
        val text = "[green]点击核心打开购买菜单[white]\n" + state.teams.getActive().filter { it.hasCore() }.joinToString("\n") {
            val rules = it.team.rules()
            "[#${it.team.color}]${it.team.name}[white]属性" + "${Iconc.blockDuo}: " + "${Iconc.defense}${rules.blockHealthMultiplier.format()}" + "${Iconc.commandAttack}${rules.blockDamageMultiplier.format()}" + "   " + "${Iconc.unitEvoke}: " + "${Iconc.commandAttack}${rules.unitDamageMultiplier.format()}"
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
