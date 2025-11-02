@file:Depends("mapScript/shared/hexed")
@file:Depends("mapScript/13545", "CoreWar", soft = true)

package mapScript

import arc.math.geom.Geometry
import arc.util.Time
import mapScript.shared.HexData
import mapScript.shared.HexedGenerator
import mindustry.game.Gamemode
import mindustry.game.Schematics
import mindustry.game.Team
import mindustry.type.Category
import mindustry.type.ItemStack
import kotlin.math.abs

/** @author WayZer
 * 私有脚本，仅供参考 */

val generator = HexedGenerator(4, 5, 78)
registerGenerator(
    "HEXed * CoreWar", "WayZer", """
        塔防掉落[@TDDrop=all]
        招兵买马(CoreWar)模式 @13545
        [@pvpProtect=0]
        """.trimIndent(),
    mode = Gamemode.pvp,
    filter = setOf("all", "display", "pvp", "hexed"),
    width = generator.width, height = generator.height
) {
    rules.apply {
        generator.applyRules(this)
        loadout = ItemStack.list(
            Items.copper, 100
        )
        repeat(generator.chunkCenters.size) {
            teams[Team.get(it + 6)].cheat = true
        }

        bannedBlocks.addAll(content.blocks().select { it.category == Category.crafting })
        bannedBlocks.addAll(
            Blocks.airFactory, Blocks.groundFactory, Blocks.navalFactory,
            Blocks.additiveReconstructor, Blocks.multiplicativeReconstructor
        )

        //balance
        blockHealthMultiplier = 1f
        buildCostMultiplier = 2f
        buildSpeedMultiplier = 2.5f
    }
    @Suppress("SpellCheckingInspection")
    val coreSchema = Schematics.readBase64(
        "bXNjaAF4nE2RWU7EMBBEy8skzjIgDhIJJG7AMRAfJhiI5IkjJwPi9lTbfBBNpuJ2PfdiDLg1sKu/BNiH+/tHDG9hn/OyHUtaATTRv4a4Qz+/3OBuTtsW8vTtY5yizx8Bw78Qzukr5PeYvqcPfwR0e9h89kfKaPfZH0fIGPZEcNr8GiLsp18ixjnlMK3XOYbrjjETWuhIy3rA+Dzj7h/zl7a5hPWNx7nrGpOXr/a1JPhhzU98YVGeDlpB8UPJT9WVgdaUBkakreJgZG9AMY/VeSNBLREywsnqJICuuK64rrhmPmUpfXUW3BA3ZCR/g3JOyzNaCMNEZAzOKHUVQErXPS08d+DKQo3860/0lPgoZksz5STmM8XCiPRQEix7jeyNFCsoW2u4bSRetlvpq6OwVEdhOivtsFxKz8unMJfUepZjW3JKJlXG6eo4XZ2Hq/NwdR6uzsPJPMQ5VmdprxNcla61NM92ZW4F7yreldFQyvV1cikixH8BA+NHDg=="
    )!!

    genRound("topography") {
        for (tile in it) {
            tile.setFloor(Blocks.darksandWater.asFloor())
            tile.setBlock(Blocks.pine)
        }
    }
    genRound("genHex", generator::genHex)
    genRound("genHexGround") { tiles ->
        generator.run {
            chunkCenters.forEach {
                Geometry.circle(it.x, it.y, 21) { nx, ny ->
                    if (abs(ny - it.y) <= 2 && abs(nx - it.x) > 7) return@circle
                    tiles.get(nx, ny).setFloor(Blocks.grass.asFloor())
                }
            }
        }
    }
    genRound("genPath", generator::genPath)
    genRound("baseOre") { tiles ->
        generator.run {
            chunkCenters.forEach {
                tiles.get(it.x - 6, it.y).setOverlay(Blocks.oreCopper)
                tiles.get(it.x + 6, it.y).setOverlay(Blocks.oreLead)
            }
        }
    }
    genRound("initHexData") { HexData.init(generator.chunkCenters, coreSchema) }
}

onEnable {
    val startTime = Time.millis()
    HexData.extraLoadout += {
        val seconds = (Time.timeSinceMillis(startTime) / 1000).toInt()
        controller.core()?.items?.apply {
            add(Items.copper, seconds * 6)
            add(Items.lead, seconds * 4)
            add(Items.silicon, (seconds - 200).coerceAtLeast(0))
            add(Items.titanium, (seconds - 300).coerceAtLeast(0) / 2)
            add(Items.thorium, (seconds - 600).coerceAtLeast(0) / 5)
        }
    }

    loop(Dispatchers.game) {
        val seconds = (Time.timeSinceMillis(startTime) / 1000).toInt()
        val produce =
            state.teams.getActive().associate {
                val produce = 300 * it.cores.size +
                        80 * it.countType(UnitTypes.mono) +
                        3 * seconds
                it.team to produce
            }
        repeat(30) {
            Groups.player.forEach { p ->
                Call.setHudTextReliable(
                    p.con, "{left}秒后发放资源: {rate}\n资源量=300*核心数+80*mono个数+时间*3".with(
                        "left" to 30 - it, "rate" to (produce[p.team()] ?: 0)
                    ).toPlayer(p)
                )
            }
            delay(1000)
        }
        produce.forEach { it.key.core()?.handleStack(Items.copper, it.value, null) }
    }
}