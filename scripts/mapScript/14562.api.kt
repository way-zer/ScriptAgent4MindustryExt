package mapScript

import mindustry.content.Blocks
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.environment.Floor

class IslandTile(val tile: Tile) {
    companion object {
        var waterFloor: Floor = Blocks.deepwater.asFloor()!!
        val discoverQueue = mutableListOf<IslandTile>()
        var tiles = emptyList<IslandTile>()
    }

    val floor: Floor = tile.floor()
    val overlay: Block = tile.overlay()
    var discovered = false

    init {
        if (overlay != Blocks.spawn && tile.block() == Blocks.air) {
            tile.setBlock(Blocks.stone)//remove ore
            tile.setFloor(waterFloor)
            tile.setAir()
        } else
            discovered = true
    }

    //use in init stage, no net sync
    fun discoverInit() {
        if (discovered) return
        discovered = true
        tile.setBlock(Blocks.stone)//remove ore
        tile.setFloor(floor)
        tile.setOverlay(overlay)
        tile.setAir()
    }

    fun discover(): Boolean {
        if (discovered) return false
        discovered = true
        tile.setFloorNet(floor, overlay)
        if (tile.block() == Blocks.air)
            tile.setAir()
        if (floor.isDeep)//auto discover
            repeat(4) {
                val tile = tile.nearby(it) ?: return@repeat
                discoverQueue.add(tiles[tile.array()])
            }
        return true
    }

    fun unDiscover(): Boolean {
        if (!discovered) return false
        discovered = false
        tile.setBlock(Blocks.stone)//remove ore
        tile.setFloorNet(waterFloor, Blocks.air)
        tile.setAir()
        return true
    }
}