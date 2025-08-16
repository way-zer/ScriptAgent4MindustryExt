@file:Depends("mapScript/shared/hexed")
@file:Depends("mapScript/tags/autoExchange", "等价交换", soft = true)

package mapScript

import arc.math.geom.Geometry
import mapScript.shared.GeneratorHelper
import mapScript.shared.HexData
import mapScript.shared.HexedGenerator
import mindustry.game.Gamemode
import mindustry.type.ItemStack
import mindustry.world.blocks.environment.Floor
import kotlin.time.Duration.Companion.minutes

/** @author WayZer */

val generator = HexedGenerator(4, 5, 144, 34)
registerGenerator(
    "HEXed PVP 超大区块", "WayZer", """插件控制的随机pvp图[@pvpProtect=480]""",
    mode = Gamemode.pvp,
    filter = setOf("all", "display", "pvp", "hexed"),
    width = generator.width, height = generator.height
) {
    rules.apply {
        generator.applyRules(this)
        loadout = ItemStack.list(
            Items.copper, 500,
            Items.lead, 500,
            Items.silicon, 200,
            Items.plastanium, 50
        )
        enemyCoreBuildRadius = 65f * tilesize
    }
    genRound("topography") { GeneratorHelper.genTopography(it) }
    genRound("genHex", generator::genHex)
    genRound("genPath", generator::genPath)
    genRound("ores", GeneratorHelper::genOres)
    genRound("baseResource") {
        generator.chunkCenters.forEach { chunk ->
            arrayOf(-20, 20).forEach { dx ->
                Geometry.circle(chunk.x + dx, chunk.y, it.width, it.height, 3) { x, y ->
                    it[x, y].setFloor(Blocks.sandWater as Floor)
                }
            }
            Geometry.circle(chunk.x, chunk.y, it.width, it.height, 15) { x, y ->
                it[x, y].setFloor(Blocks.sand as Floor)
            }
        }
    }
    genRound("genRandomStone", GeneratorHelper::genRandomStone)
    genRound("initHexData") { HexData.init(generator.chunkCenters) }
}


onEnable {
    HexData.extraLoadout.add {
        val tileSize = tilesize.toFloat()
        repeat(2) {
            UnitTypes.mono.spawn(controller, x * tileSize, y * tileSize).apply {
                armor = 200f
            }
        }
    }

    schedule(20.minutes) {
        HexData.extraLoadout.add {
            val tileSize = tilesize.toFloat()
            repeat(4) {//all 6 mono
                UnitTypes.mono.spawn(controller, x * tileSize, y * tileSize).apply {
                    armor = 200f
                }
            }
            repeat(2) {
                UnitTypes.poly.spawn(controller, x * tileSize, y * tileSize).apply {
                    armor = 200f
                }
            }
        }
    }
    schedule(40.minutes) {
        HexData.extraLoadout.add {
            val tileSize = tilesize.toFloat()
            repeat(2) {
                UnitTypes.vela.spawn(controller, x * tileSize, y * tileSize).apply {
                    armor = 100f
                }
            }
        }
    }
    schedule(60.minutes) {
        HexData.extraLoadout.add {
            val items = coreTile.build?.items ?: return@add
            content.items().toMutableSet().apply {
                removeAll(setOf(Items.blastCompound, Items.surgeAlloy, Items.surgeAlloy, Items.phaseFabric))
            }.forEach { items.set(it, coreTile.block().itemCapacity) }
        }
    }
}