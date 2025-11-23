@file:Depends("mapScript/shared/hexed")

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
import org.intellij.lang.annotations.Language
import kotlin.time.Duration.Companion.minutes

/** @author WayZer */

val coreSchema: Schematic =
    Schematics.readBase64(
        "bXNjaAF4nE2Kyw3CMBBEx5+EBBBUkooiDmaziCDHjmxH4k4jlMGNaqAONjc00nz0BhW2Bja4idFe+f59vj7vB3YDZ0rjXMYYANTendln6P5U43iJibibU7wxlZiwp5i4Cwt5XjIOE4fhjzZL8NENnGBcImwyuVJkAS2gREZBo4IY0EAbKY1Mpa20FlrDCFEWK1GVhLwsVmp+DXcoHQ=="
    )

val generator = HexedGenerator(spacing = 88, wallWidth = 5)
registerGenerator(
    "HEXed*[blue]海洋领域[]*[gold]无限火力", "WayZer", """
            插件控制的随机pvp图[@pvpProtect=0]
            无限火力模式
            合金炮伤害*0.25,浪涌删除塑钢子弹
            关闭单位死亡的物品爆炸伤害""".trimIndent(),
    mode = Gamemode.pvp,
    filter = setOf("all", "display", "pvp", "hexed"),
    width = generator.width, height = generator.height
) {
    rules.apply {
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
    }
    genRound("topography") { tiles ->
        for (tile in tiles) {
            tile.setFloor(Blocks.slag as Floor)
        }
    }
    genRound("genHex") { tiles ->
        with(generator) {
            val d = (spacing - wallWidth) * 2 / Mathf.sqrt3
            chunkCenters.forEach {
                hexShape(it.x, it.y, d) { x, y -> tiles[x, y].setFloor(Blocks.deepwater as Floor) }
                hexShape(it.x, it.y, d * 7 / 9) { x, y -> tiles[x, y].setFloor(Blocks.sandWater as Floor) }
                hexShape(it.x, it.y, d * 5 / 9) { x, y -> tiles[x, y].setFloor(Blocks.sand as Floor) }
            }
        }
    }
    genRound("genPath") { tiles ->
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
    genRound("ores", GeneratorHelper::genOres)
    genRound("genRandomStone", GeneratorHelper::genRandomStone)
    genRound("initHexData") { HexData.init(generator.chunkCenters, coreSchema) }
}

@Language("JSON5")
val patch = """
{
  "name": "Hexed-1001-Balance",
  "block.core-nucleus.unitType": "emanate",
  "block.foreshadow.ammoTypes.surge-alloy.damage": 350, // origin 1350
  "block.ripple.ammoTypes.plastanium": "-",
}
""".trimIndent()
mapPatches = listOf(patch)

onEnable {
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