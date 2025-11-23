@file:Depends("mapScript/shared/hexed")

package mapScript

import mapScript.shared.GeneratorHelper
import mapScript.shared.HexData
import mapScript.shared.HexedGenerator
import mindustry.game.Gamemode
import mindustry.game.Schematic
import mindustry.game.Schematics
import mindustry.game.Team
import mindustry.type.ItemStack
import kotlin.time.Duration.Companion.minutes

/** @author WayZer */

val coreSchema: Schematic = Schematics.readBase64(
    "bXNjaAF4nC2NWw6CMBBFLw95Gv1wHazI+FHKRKojJaVI3L1DSyaZMz2906JFmyGf1IeQeLQDLdqZ2Rs7AShY9cQL0vujRNuvhofO240cit6R0iMuPbkfs1k/3aaYcdbWUaeNVwMxqnViK5PD1dGTpm529kXaWxHLaPV7U186XgRuQCIVWg2kyEQGJBEppJ2kkMtQRXkkG4QbSRaCIMu4Xu4yF6TxVEUckQbht2TP1rv8A96+LDU="
)

val generator = HexedGenerator(4, 5, 96, 11, 9)
registerGenerator(
    "HEXed 实验版(双科无限火力)", "WayZer", """
            插件控制的随机pvp图[@pvpProtect=0]
            无限火力模式
            关闭单位死亡的物品爆炸伤害""".trimIndent(),
    mode = Gamemode.pvp,
    filter = setOf("all", "display", "pvp", "hexed"),
    width = generator.width, height = generator.height
) {
    rules.apply {
        generator.applyRules(this)
        Planets.sun.applyRules(this)
        loadout = ItemStack.list(
            Items.copper, 2000,
            Items.lead, 2000,
            Items.graphite, 300,
            Items.silicon, 150,
            Items.plastanium, 50,
            Items.beryllium, 2000,
        )
        bannedBlocks.addAll(
            //生产调控
            Blocks.smallDeconstructor, Blocks.deconstructor, Blocks.largeConstructor,
            Blocks.coreFoundation, Blocks.coreBastion,
            Blocks.overdriveDome, Blocks.overdriveProjector,
            Blocks.surgeSmelter, Blocks.phaseWeaver, Blocks.siliconArcFurnace,
            Blocks.heatRedirector, Blocks.waterExtractor,
            //平衡调控
            Blocks.mender, Blocks.mendProjector, Blocks.buildTower,
            Blocks.malign, Blocks.smite, Blocks.scathe,
            Blocks.repairPoint, Blocks.blastMixer, Blocks.pyratiteMixer,
        )
        bannedUnits.add(UnitTypes.mono)
        damageExplosions = false

        //balance
        blockHealthMultiplier = 2f
        unitDamageMultiplier = 0.8f
        blockDamageMultiplier = 0.5f
        unitBuildSpeedMultiplier = 2f
        buildCostMultiplier = 2f
        buildSpeedMultiplier = 2f

        repeat(generator.chunkCenters.size) {
            teams[Team.get(it + 6)].cheat = true
        }
    }
    genRound("topography") { GeneratorHelper.genTopography(it) }
    genRound("genHex", generator::genHex)
    genRound("genPath", generator::genPath)
    genRound("ores") {
        GeneratorHelper.genOres(
            it, ores = GeneratorHelper.defaultOres + arrayOf(
                GeneratorHelper.oreFilter(Blocks.oreBeryllium, 24f, 0.8f),
                GeneratorHelper.oreFilter(Blocks.oreTungsten, 24f, 0.8f),
            )
        )
    }
    genRound("initHexData") { HexData.init(generator.chunkCenters, coreSchema) }
    genRound("coreFloor") { tiles ->
        for (center in generator.chunkCenters) {
            tiles[center.x, center.y].getLinkedTilesAs(Blocks.coreNucleus) {
                it.setFloor(Blocks.coreZone.asFloor())
            }
        }
    }
}

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
                    armor = 30f
                }
            }
        }
    }
    schedule(30.minutes) {
        HexData.extraLoadout.add {
            val tileSize = tilesize.toFloat()
            repeat(4) {
                UnitTypes.vela.spawn(controller, x * tileSize, y * tileSize).apply {
                    armor = 50f
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