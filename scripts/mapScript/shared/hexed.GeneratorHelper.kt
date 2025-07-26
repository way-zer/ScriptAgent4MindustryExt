package mapScript.shared

import arc.math.Mathf
import arc.util.noise.Simplex
import mindustry.content.Blocks.*
import mindustry.maps.filters.GenerateFilter
import mindustry.maps.filters.OreFilter
import mindustry.world.Block
import mindustry.world.Tiles
import mindustry.world.blocks.environment.Floor

@Suppress("MemberVisibilityCanBePrivate", "unused")
object GeneratorHelper {
    val defaultFloors = arrayOf(
        arrayOf(sand, sand, sand, sand, sand, grass),
        arrayOf(darksandWater, darksand, darksand, darksand, grass, grass),
        arrayOf(darksandWater, darksand, darksand, darksand, grass, shale),
        arrayOf(darksandTaintedWater, darksandTaintedWater, moss, moss, sporeMoss, stone),
        arrayOf(ice, iceSnow, snow, dacite, hotrock, salt)
    )
    val defaultBlocks = arrayOf(
        arrayOf(stoneWall, stoneWall, sandWall, sandWall, pine, pine),
        arrayOf(stoneWall, stoneWall, duneWall, duneWall, pine, pine),
        arrayOf(stoneWall, stoneWall, duneWall, duneWall, pine, pine),
        arrayOf(sporeWall, sporeWall, sporeWall, sporeWall, sporeWall, stoneWall),
        arrayOf(iceWall, snowWall, snowWall, snowWall, stoneWall, saltWall)
    )

    fun genTopography(
        tiles: Tiles,
        floors: Array<Array<Block>> = defaultFloors,
        blocks: Array<Array<Block>> = defaultBlocks
    ) {
        val seed1 = Mathf.random(0, 10000)
        val seed2 = Mathf.random(0, 10000)
        for (tile in tiles) {
            val x = tile.x
            val y = tile.y
            val temp = Simplex.noise2d(seed1, 12.0, 0.6, 1.0 / 400, x.toDouble(), y.toDouble())
                .let { (((it - 0.5) * 10 + 0.15f) * blocks.size).toInt() }
                .coerceIn(0, blocks.size - 1)
            val elev = Simplex.noise2d(seed2, 12.0, 0.6, 1.0 / 700, x.toDouble(), y.toDouble())
                .let { (((it - 0.5) * 10 + 0.15f) * blocks[0].size).toInt() }
                .coerceIn(0, blocks[0].size - 1)
            tile.setFloor(floors[temp][elev] as Floor)
            tile.setBlock(blocks[temp][elev])
        }
    }

    val defaultOres = arrayOf(
        oreFilter(oreCopper, 24f, 0.8f),
        oreFilter(oreLead, 24f, 0.8f),
        oreFilter(oreCoal, 24f, 0.8f),
        oreFilter(oreTitanium, 24f, 0.8f),
        oreFilter(oreThorium, 24f, 0.8f),
    )

    fun genOres(tiles: Tiles, ores: Array<OreFilter> = defaultOres) {
        val input = GenerateFilter.GenerateInput().apply {
            floor = stone
            height = tiles.height
            width = tiles.width
        }
        ores.forEach { it.randomize() }
        for (tile in tiles) {
            input.block = tile.block();input.overlay = air
            input.x = tile.x.toInt();input.y = tile.y.toInt()
            ores.forEach { it.apply(input) }
            tile.setOverlay(input.overlay)
        }
    }

    fun genRandomStone(tiles: Tiles) {
        for (tile in tiles) {
            if (tile.block() === air && Mathf.chance(0.03)) {
                tile.setBlock(
                    when (tile.floor()) {
                        sand -> sandBoulder
                        stone -> boulder
                        shale -> shaleBoulder
                        darksand -> boulder
                        moss -> sporeCluster
                        ice -> snowBoulder
                        snow -> snowBoulder
                        else -> continue
                    }
                )
            }
        }
    }

    //util
    fun oreFilter(ore: Block, scl: Float, threshold: Float) = OreFilter().apply {
        this.ore = ore
        this.scl = scl
        this.threshold = threshold
    }
}