package mapScript

import arc.math.Mathf
import arc.math.geom.Geometry
import arc.struct.IntSeq
import coreLibrary.lib.util.loop
import coreLibrary.lib.util.reflectDelegate
import mindustry.ai.BlockIndexer
import mindustry.content.Blocks
import mindustry.gen.Groups
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.environment.OreBlock
import mindustry.world.blocks.production.Drill
import kotlin.random.Random

/**
 * Contributors: BlackDeluxeCat, Wayzer
 * */
name = "生长矿物"
//process rate per run
val growRate = 0.9     //if ore spreads to a pebbles
val spreadRate = 0.2   //if ore spreads to a no-pebbles overlay
val decayRate = 0.8    //ore turns into pebbles
val dryupRate = 0.1    //pebbles disappear
val BlockIndexer.ores: Array<Array<Array<IntSeq?>>?> by reflectDelegate()

fun Tile.setOre(target: Block) {
    val drop = drop()
    if (drop != null) {
        indexer.ores[drop.id.toInt()]?.get(x / 20)?.get(y / 20)?.removeValue(pos())
        if (block() == Blocks.air) {
            setNet(Blocks.boulder)
            launch(Dispatchers.game) { //delay set for unit
                delay(5000L)
                setOre(target)
                if (block() == Blocks.boulder) removeNet()
            }
            return
        }
    }
    setFloorNet(floor(), target)
    indexer.addIndex(this)
}

fun Tile.closeToDrill(): Boolean {
    return build?.block() is Drill ||
            Geometry.d4.any { nearby(it.x, it.y)?.build?.block() is Drill }
}

fun growOre(tile: Tile) {
    if (tile.overlay() !is OreBlock || tile.closeToDrill()) return
    for (p in Geometry.d4) {
        val other = world.tiles.get(tile.x + p.x, tile.y + p.y) ?: continue
        if (other.build != null) continue
        when {
            other.overlay() == Blocks.pebbles && Mathf.chance(growRate) -> other.setOre(tile.overlay())
            other.overlay() !is OreBlock && Mathf.chance(spreadRate) -> other.setOre(tile.overlay())
        }
    }
}

onEnable {
    loop(Dispatchers.game) {
        if (!state.isPaused) {
            repeat(8) {
                val tile = world.tiles.getn(Random.nextInt(world.width()), Random.nextInt(world.height()))
                if (tile.closeToDrill()) {
                    when {
                        tile.overlay() is OreBlock && Mathf.chance(decayRate) -> tile.setOre(Blocks.pebbles)
                        tile.overlay() == Blocks.pebbles && Mathf.chance(dryupRate) -> tile.setOre(Blocks.air)
                    }
                } else growOre(tile)
            }
        }
        yield()
    }
    loop(Dispatchers.game) {
        delay(5000L)
        if (!state.isPaused)
            Groups.unit.each {
                val tile = it.mineTile() ?: return@each
                if (tile.overlay() !is OreBlock) return@each
                when {
                    Mathf.chance(decayRate) -> tile.setOre(Blocks.pebbles)
                    Mathf.chance(dryupRate) -> tile.setOre(Blocks.air)
                }
            }
    }
}