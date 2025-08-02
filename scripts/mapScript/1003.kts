@file:Depends("mapScript/shared/hexed")
@file:Depends("coreMindustry/utilMapRule", "参数平衡")

package mapScript

import arc.math.Mathf
import mapScript.shared.GeneratorHelper
import mapScript.shared.HexData
import mapScript.shared.HexedGenerator
import mindustry.game.Gamemode
import mindustry.game.Schematic
import mindustry.game.Schematics
import mindustry.game.Team
import mindustry.type.ItemStack
import mindustry.world.blocks.defense.turrets.ItemTurret
import mindustry.world.blocks.environment.Floor
import kotlin.time.Duration.Companion.minutes

/** @author WayZer
 * 私有脚本，仅供参考 */
name = "HEXed*海洋领域*无限火力"

val coreSchema: Schematic =
    Schematics.readBase64(
        "bXNjaAF4nE2Kyw3CMBBEx5+EBBBUkooiDmaziCDHjmxH4k4jlMGNaqAONjc00nz0BhW2Bja4idFe+f59vj7vB3YDZ0rjXMYYANTendln6P5U43iJibibU7wxlZiwp5i4Cwt5XjIOE4fhjzZL8NENnGBcImwyuVJkAS2gREZBo4IY0EAbKY1Mpa20FlrDCFEWK1GVhLwsVmp+DXcoHQ=="
    )

val generator = HexedGenerator(spacing = 88, wallWidth = 5)
mapMode = Gamemode.pvp
mapFilters = setOf("all", "display", "pvp", "hexed")
setMapInfo(generator.width, generator.height, tagsApply = {
    put("name", "HEXed*[blue]海洋领域[]*[gold]无限火力")
    put("author", "WayZer")
    put(
        "description", """
            插件控制的随机pvp图[@pvpProtect=0]
            无限火力模式
            合金炮伤害*0.25,浪涌删除塑钢子弹
            关闭单位死亡的物品爆炸伤害""".trimIndent()
    )
}, rulesApply = {
    generator.applyRules(this)
    loadout = ItemStack.list(
        Items.copper, 2000,
        Items.lead, 2000,
        Items.graphite, 300,
        Items.silicon, 150,
        Items.plastanium, 50
    )
    bannedBlocks.addAll(Blocks.blastMixer, Blocks.mendProjector, Blocks.deconstructor, Blocks.largeConstructor)
    damageExplosions = false

    //balance
    blockHealthMultiplier = 2f
    blockDamageMultiplier = 1.3f
    unitDamageMultiplier = 0.7f
    unitBuildSpeedMultiplier = 0.8f
    buildCostMultiplier = 2f
    buildSpeedMultiplier = 2.5f

    repeat(generator.chunkCenters.size) {
        teams[Team.get(it + 6)].cheat = true
    }
})

genRound += "topography" to { tiles ->
    for (tile in tiles) {
        tile.setFloor(Blocks.slag as Floor)
    }
}
genRound += "genHex" to { tiles ->
    with(generator) {
        val d = (spacing - wallWidth) * 2 / Mathf.sqrt3
        chunkCenters.forEach {
            hexShape(it.x, it.y, d) { x, y -> tiles[x, y].setFloor(Blocks.deepwater as Floor) }
            hexShape(it.x, it.y, d * 7 / 9) { x, y -> tiles[x, y].setFloor(Blocks.sandWater as Floor) }
            hexShape(it.x, it.y, d * 5 / 9) { x, y -> tiles[x, y].setFloor(Blocks.sand as Floor) }
        }
    }
}
genRound += "genPath" to { tiles ->
    with(generator) {
        chunkCenters.forEach { chunk ->
            chunkCenters.filter { it != chunk && it.dst(chunk) < spacing * 1.1 }.forEach {
                lineShape(chunk, it, 5) { lx, ly ->
                    val tile = tiles.getn(lx, ly)
                    if (tile.floor() != Blocks.sand)
                        tile.setFloor(Blocks.sandWater as Floor)
                }
            }
        }
    }
}
genRound += "ores" to { GeneratorHelper.genOres(it) }
genRound += "genRandomStone" to GeneratorHelper::genRandomStone
genRound += "initHexData" to { HexData.init(generator.chunkCenters, coreSchema) }

val mapRule = contextScript<coreMindustry.UtilMapRule>()
onEnable {
    mapRule.registerMapRule(UnitTypes.gamma::health) { 2000f }
    mapRule.registerMapRule(UnitTypes.gamma::armor) { 500f }
    mapRule.registerMapRule((Blocks.foreshadow as ItemTurret).ammoTypes[Items.surgeAlloy]::damage) { it / 4 }
    mapRule.registerMapRule((Blocks.ripple as ItemTurret)::ammoTypes) {
        it.copy().apply { remove(Items.plastanium) }
    }
    HexData.extraLoadout.add {
        val tileSize = tilesize.toFloat()
        repeat(6) {
            UnitTypes.mono.spawn(controller, x * tileSize, y * tileSize).apply {
                armor = 200f
            }
        }
    }

    schedule(5.minutes) { state.rules.loadout.add(ItemStack(Items.titanium, 1000)) }
    schedule(10.minutes) { state.rules.loadout.add(ItemStack(Items.thorium, 1000)) }
    schedule(15.minutes) { state.rules.loadout.add(ItemStack(Items.surgeAlloy, 1000)) }
    schedule(20.minutes) {
        HexData.extraLoadout.add {
            val tileSize = tilesize.toFloat()
            repeat(4) {
                UnitTypes.mega.spawn(controller, x * tileSize, y * tileSize).apply {
                    armor = 200f
                }
            }
        }
    }
    schedule(30.minutes) {
        HexData.extraLoadout.add {
            val tileSize = tilesize.toFloat()
            repeat(4) {
                UnitTypes.vela.spawn(controller, x * tileSize, y * tileSize).apply {
                    armor = 200f
                }
            }
        }
    }
    schedule(40.minutes) {
        HexData.extraLoadout.add {
            val items = coreTile.build?.items ?: return@add
            content.items().forEach {
                if (it == Items.blastCompound) return@forEach
                items.set(it, coreTile.block().itemCapacity)
            }
        }
    }
}