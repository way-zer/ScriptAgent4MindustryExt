package mapScript

import arc.math.Mathf
import cf.wayzer.scriptAgent.contextScript
import coreLibrary.lib.with
import coreMindustry.MenuBuilder
import coreMindustry.lib.MsgType
import coreMindustry.lib.broadcast
import coreMindustry.util.spawnAround
import mindustry.Vars
import mindustry.ai.types.MinerAI
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.game.Team
import mindustry.gen.Building
import mindustry.gen.Iconc
import mindustry.gen.Player
import mindustry.type.UnitType
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.reflect.KMutableProperty0

private val confirmed = mutableSetOf<String>()

class CoreWarMenu(val player: Player, val build: Building) : MenuBuilder<Unit>(followup = true) {
    private val script = contextScript<_13545>()
    private val isErekir = build.block in arrayOf(Blocks.coreAcropolis, Blocks.coreBastion, Blocks.coreCitadel)
    private val item = if (isErekir) Items.beryllium else Items.copper
    private fun Building.getResource() = items.get(item)
    private fun Building.removeResource(v: Int) = items.remove(item, v)
    private val Team.myData get() = with(script) { myData }

    @MenuBuilderDsl
    suspend fun costOption(title: String, cost: () -> Int, body: () -> Boolean) = lazyOption {
        val costV = cost()
        option("$title\n${item.emoji()}${cost()}")

        if (build.team != player.team())
            return@lazyOption player.sendMessage("[red]核心丢失，请重新打开")
        if (build.getResource() < costV)
            return@lazyOption player.sendMessage("[red]资源不足购买资源, 需要: $costV")

        val important = costV > build.getResource() * 0.1
        if (important && player.uuid() !in confirmed) {
            MenuBuilder<Unit>("温馨提示") {
                msg = "本次购买花费过多，为了不必要的争议，请再次确认\n" +
                        "尤其是队伍打算攒钱购买高级单位时，乱花钱可能导致踢出"
                option("不再提示") { confirmed.add(player.uuid()) }
                option("返回") { }
            }.sendTo(player, 60_000)
            refresh()
        }

        if (!body()) return@lazyOption player.sendMessage("[red]购买失败")
        build.removeResource(costV)
        val msg = "{player.name}花费了${item.emoji()}{cost}购买了{title}"
            .with("player" to player, "cost" to costV, "title" to title.replace('\n', ' '))
        broadcast(msg, MsgType.InfoToast, quite = true, players = build.team.data().players)
        if (important)
            broadcast(msg, quite = true, players = build.team.data().players)
        refresh()
    }

    @MenuBuilderDsl
    suspend fun upgradeOption(
        title: String,
        value: KMutableProperty0<Float>,
        cost: (Int) -> Int
    ) {
        fun Float.fix() = (this * 100).roundToInt() / 100f
        val delta = 0.05f
        val now = value.get()
        fun lvl() = ((value.get() - 1) / delta).roundToInt()
        costOption("$title\n($now -> ${(now + delta).fix()})", { cost(lvl()) }) {
            value.set((1 + delta * (lvl() + 1)).fix())
            true
        }
    }

    val unitCostRate get() = Vars.state.rules.tags.getFloat("@unitCost", 1.0f)
    private fun costRate(count: Int) = when {
        count <= 10 -> (1 + (count * 0.1).pow(3))//when 10, cost is 8x
        count <= 16 -> (count * 0.25).pow(2) //when 16, cost is 16x
        count <= 20 -> (count * 0.4).pow(1.5) //when 20, cost is 22.6x
        else -> 20.0
    }
    @MenuBuilderDsl
    suspend fun unitOption(type: UnitType, baseCost: Int) {
        val electrifiedTime = Mathf.sqrt(baseCost.toFloat()).coerceAtMost(180f)
        fun count() = player.team().data().countType(type)
        fun cost() = unitCostRate * baseCost * costRate(count())
        costOption(
            "${type.emoji()}\n${Iconc.statusElectrified}${electrifiedTime.roundToInt()}s",
            { cost().roundToInt() }) {
            type.spawnAround(build, build.team)?.apply {
                apply(StatusEffects.electrified, electrifiedTime * 60f)
                apply(StatusEffects.disarmed, electrifiedTime * 60f / 3)
            } != null
        }
    }

    val noConvertCopper get() = Vars.state.rules.tags.containsKey("@noConvertCopper")
    override suspend fun build() {
        title = "招兵买马"
        msg = """
            [yellow]可用资源: ${item.emoji()}${build.getResource()}
            [green]点击按钮购买对应队伍和升级
        """.trimIndent()
        upgradeOption("升级建筑攻击", player.team().myData::blockDamageMultiplier) { 100 + it * 180 }
        upgradeOption("升级建筑血量", player.team().myData::blockHealthMultiplier) { 300 + it * 260 }

        newRow()
        upgradeOption("升级单位攻击", player.team().myData::unitDamageMultiplier) { 100 + it * 200 }
        upgradeOption("升级单位血量", player.team().myData::unitHealthMultiplier) { 500 + it * 300 }

        newRow()
        if (!noConvertCopper)
            option("${Iconc.itemLead}${Iconc.itemSand}${Iconc.itemCoal}${Iconc.itemScrap} -> ${Iconc.itemCopper}") {
                build.items.apply {
                    arrayOf(Items.lead, Items.scrap, Items.sand, Items.coal).forEach {
                        add(Items.copper, get(it))
                        set(it, 0)
                    }
                }
                refresh()
            }
        fun monoCount() = player.team().data().countType(UnitTypes.mono)
        costOption("${Iconc.unitMono}", { (200 * (1 + (monoCount() * 0.2).pow(1.5))).roundToInt() }) {
            UnitTypes.mono.spawnAround(build, build.team)?.apply {
                controller(MonoAI())
                apply(StatusEffects.invincible, Float.MAX_VALUE)
            } != null
        }

        newRow()
        option("${Iconc.itemCopper} <--") {
            build.items.apply {
                val delta = (get(Items.beryllium) + 1)
                remove(Items.beryllium, delta)
                add(Items.copper, delta)
            }
            refresh()
        }
        option("--> ${Iconc.itemBeryllium}") {
            build.items.apply {
                val delta = (get(Items.copper) + 1)
                remove(Items.copper, delta)
                add(Items.beryllium, delta)
            }
            refresh()
        }

        newRow()
        if (!isErekir) {
            unitOption(UnitTypes.crawler, 42)
            unitOption(UnitTypes.flare, 50)
            unitOption(UnitTypes.risso, 70)
            unitOption(UnitTypes.retusa, 110)

            newRow()
            unitOption(UnitTypes.mace, 100)
            unitOption(UnitTypes.atrax, 125)
            unitOption(UnitTypes.minke, 140)
            unitOption(UnitTypes.horizon, 120)

            newRow()
            unitOption(UnitTypes.fortress, 450)
            unitOption(UnitTypes.bryde, 850)
            unitOption(UnitTypes.zenith, 550)

            newRow()
            unitOption(UnitTypes.scepter, 5000)
            unitOption(UnitTypes.antumbra, 6000)
            unitOption(UnitTypes.sei, 9500)

            newRow()
            unitOption(UnitTypes.vela, 18000)
            unitOption(UnitTypes.arkyid, 12000)

            newRow()
            unitOption(UnitTypes.reign, 52000)
            unitOption(UnitTypes.corvus, 67000)

            newRow()
            unitOption(UnitTypes.eclipse, 58000)
            unitOption(UnitTypes.omura, 75000)
        } else {
            unitOption(UnitTypes.stell, 100)
            unitOption(UnitTypes.merui, 130)
            unitOption(UnitTypes.elude, 200)
            newRow()
            unitOption(UnitTypes.locus, 450)
            unitOption(UnitTypes.cleroi, 700)
            unitOption(UnitTypes.avert, 480)
            newRow()
            unitOption(UnitTypes.precept, 3000)
            unitOption(UnitTypes.anthicus, 4900)
            unitOption(UnitTypes.obviate, 4350)
            newRow()
            unitOption(UnitTypes.vanquish, 6000)
            unitOption(UnitTypes.tecta, 8000)
            unitOption(UnitTypes.quell, 8800)
            newRow()
            unitOption(UnitTypes.conquer, 45000)
            unitOption(UnitTypes.collaris, 80000)
            unitOption(UnitTypes.disrupt, 70000)
        }

        newRow()
        option("关闭") {}
    }

    class MonoAI : MinerAI() {
        override fun isLogicControllable(): Boolean = false
        override fun updateMovement() {
            timer.reset(timerTarget2, 0f)//关闭自动切换
            targetItem = Items.copper
            super.updateMovement()
        }
    }
}