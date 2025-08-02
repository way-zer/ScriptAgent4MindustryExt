@file:Depends("mapScript/shared/hexed")
@file:Depends("coreMindustry/utilMapRule", "参数平衡")

package mapScript

import mapScript.shared.GeneratorHelper
import mapScript.shared.HexData
import mapScript.shared.HexedGenerator
import mindustry.game.Gamemode
import mindustry.game.Schematic
import mindustry.game.Schematics
import mindustry.game.Team
import mindustry.type.ItemStack
import mindustry.world.blocks.defense.turrets.ItemTurret
import mindustry.world.blocks.storage.CoreBlock
import kotlin.time.Duration.Companion.minutes

/** @author WayZer
 * 私有脚本，仅供参考 */
name = "HEXed PVP 无限火力"

val coreSchema: Schematic by lazy {
    Schematics.readBase64(
        "bXNjaAF4nE2Kyw3CMBBEx5+EBBBUkooiDmaziCDHjmxH4k4jlMGNaqAONjc00nz0BhW2Bja4idFe+f59vj7vB3YDZ0rjXMYYANTendln6P5U43iJibibU7wxlZiwp5i4Cwt5XjIOE4fhjzZL8NENnGBcImwyuVJkAS2gREZBo4IY0EAbKY1Mpa20FlrDCFEWK1GVhLwsVmp+DXcoHQ=="
    )
}

val generator = HexedGenerator()
mapMode = Gamemode.pvp
mapFilters = setOf("all", "display", "pvp", "hexed")
setMapInfo(generator.width, generator.height, tagsApply = {
    put("name", "HEXed PVP 无限火力")
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
    bannedBlocks.addAll(
        Blocks.blastMixer, Blocks.mendProjector,
        Blocks.deconstructor, Blocks.largeConstructor, Blocks.swarmer,
        Blocks.coreShard,
    )
    damageExplosions = false

    //balance
    blockHealthMultiplier = 2f
    unitDamageMultiplier = 0.7f
    unitBuildSpeedMultiplier = 0.9f
    buildCostMultiplier = 2f
    buildSpeedMultiplier = 2.5f

    repeat(generator.chunkCenters.size) {
        teams[Team.get(it + 6)].cheat = true
    }
})
genRound += "topography" to { GeneratorHelper.genTopography(it) }
genRound += "genHex" to generator::genHex
genRound += "genPath" to generator::genPath
genRound += "ores" to { GeneratorHelper.genOres(it) }
genRound += "genRandomStone" to GeneratorHelper::genRandomStone
genRound += "initHexData" to { HexData.init(generator.chunkCenters, coreSchema) }
genRound += "coreFloor" to { tiles ->
    for (center in generator.chunkCenters) {
        tiles[center.x, center.y].getLinkedTilesAs(Blocks.coreNucleus) {
            it.setFloor(Blocks.coreZone.asFloor())
        }
    }
}

val mapRule = contextScript<coreMindustry.UtilMapRule>()
onEnable {
    mapRule.registerMapRule((Blocks.coreNucleus as CoreBlock)::unitType) { UnitTypes.emanate }
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