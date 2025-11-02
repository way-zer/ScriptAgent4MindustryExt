@file:Depends("mapScript/shared/hexed")
@file:Depends("mapScript/14562", "填海造陆")

package mapScript

import mapScript.shared.HexData
import mapScript.shared.HexedGenerator
import mindustry.game.Gamemode
import mindustry.game.Schematic
import mindustry.game.Schematics
import mindustry.game.Team
import mindustry.type.Category
import mindustry.type.ItemStack
import org.intellij.lang.annotations.Language

/** @author WayZer
 * 私有脚本，仅供参考 */

val generator = HexedGenerator(spacing = 88, wallWidth = 5)
registerGenerator(
    "HEXed*[yellow]沙滩海战", "WayZer", """
        你落到了一个满是水的星球，
        不过我们有传统异能-[sky]填海造陆[]
        
        另外我们有先进的技术，能够从空气中获取能量-[sky]无限火力[]
        并且只需要一点点引子就能量转换为物质-[sky](几乎)免费建筑[]
        但是纯能量速度毕竟有点慢
        
        [@pvpProtect=0]
        平衡调整：削弱t4海辅治疗能力, 大治疗治疗量等于小治疗(范围更大)
        中泵30玻璃10合金，大泵30氧化物
        建筑塔200氧化物, 超速100相知物500氧化物
        [red]Beta平衡调整
        """.trimIndent(),
    mode = Gamemode.pvp,
    filter = setOf("all", "display", "pvp", "hexed"),
    width = generator.width, height = generator.height
) {
    rules.apply {
        generator.applyRules(this)
        loadout = ItemStack.list(
            Items.sand, 2000, Items.thorium, 10
        )

        revealedBlocks.add(Blocks.buildTower)
        hideBannedBlocks = true
        bannedBlocks.addAll(Blocks.afflict, Blocks.unitCargoLoader)

        bannedUnits.addAll(content.units().select { it.buildSpeed > 0 && it.flying })
        bannedBlocks.addAll(Blocks.overdriveDome, Blocks.mender)
        bannedBlocks.addAll(content.blocks().select { it.category == Category.distribution })
        damageExplosions = false

        //balance
        blockHealthMultiplier = 1f
        unitBuildSpeedMultiplier = 2f
        buildCostMultiplier = 0.01f
        buildSpeedMultiplier = 0.02f

        repeat(generator.chunkCenters.size) {
            teams[Team.get(it + 6)].cheat = true
        }
    }
    @Suppress("SpellCheckingInspection")
    val coreSchema: Schematic =
        Schematics.readBase64(
            "bXNjaAF4nE2Kyw3CMBBEx5+EBBBUkooiDmaziCDHjmxH4k4jlMGNaqAONjc00nz0BhW2Bja4idFe+f59vj7vB3YDZ0rjXMYYANTendln6P5U43iJibibU7wxlZiwp5i4Cwt5XjIOE4fhjzZL8NENnGBcImwyuVJkAS2gREZBo4IY0EAbKY1Mpa20FlrDCFEWK1GVhLwsVmp+DXcoHQ=="
        )

    genRound("topography") { tiles ->
        tiles.forEach { it.setFloor(Blocks.sandWater.asFloor()) }
    }
    genRound("initHexData") { HexData.init(generator.chunkCenters, coreSchema) }
}

val base = contextScript<_14562>()

@Language("JSON5")
val patch = """{
    "name": "1005",
    "unit.aegires.abilities.0.healPercent": 0.4,
    "block.mechanical-pump.buildTime": 60,//origin 22.5
    "block.rotary-pump.requirements": ["metaglass/3000","surge-alloy/1000"],
    "block.rotary-pump.buildTime": 100,
    "block.impulse-pump.requirements": ["oxide/5000"],
    "block.impulse-pump.buildTime": 1,
    "block.mend-projector.healPercent": 4,//小治疗恢复量
    "block.build-tower.unitType.buildSpeed": 1, //origin 1.5 gamma 1.0
    "block.build-tower.buildTime": 1000,//origin 234
    "block.build-tower.requirements": ["oxide/20000"],
    "block.build-tower.consumes": {remove: "all"},
    "block.overdrive-projector.buildTime": 1,
    "block.overdrive-projector.requirements": ["phase-fabric/10000","oxide/50000"],
    }
""".trimMargin()
mapPatches = listOf(patch)

onEnable {
    //        ${content.blocks().toList().filter { it is GenericCrafter || it is Separator }.joinToString("\n") {
    //            """block.${it.name}.consumers:{clearItems:1,clearLiquids:1},"""
    //        }}
    HexData.extraLoadout.add {
        coreTile.getLinkedTiles { base.myTiles[it.array()].discover() }
    }
    loop(Dispatchers.game) {
        delay(1000)
        val produce = state.teams.getActive().associate { it.team to it.cores.size }
        produce.forEach { it.key.core()?.handleStack(Items.oxide, it.value, null) }

        val top = produce.entries
            .sortedByDescending { it.value }
            .run { subList(0, size.coerceAtMost(3)) }
            .map { "{team}: +{rate}/s".with("team" to it.key, "rate" to it.value) }
            .joinToString("\n")
        Groups.player.forEach { p ->
            Call.setHudTextReliable(
                p.con, """
                {item}产量: +{rate}/s
                (核心越多产量越高)
                {top}
            """.trimIndent().with(
                    "item" to Items.oxide, "rate" to (produce[p.team()] ?: 0), "top" to top
                ).toPlayer(p)
            )
        }
    }
}