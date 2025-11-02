package mapScript

import mindustry.ctype.ContentType
import mindustry.game.EventType.Trigger
import mindustry.gen.Iconc
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.environment.Floor

name = "填海造陆"
modeIntroduce(
    "填海造陆", """
    * 使用${Iconc.blockMechanicalPump}水泵(小)开扩地图
    * 使用${Iconc.blockRotaryPump}水泵(中)开扩一片地图
    * 使用${Iconc.blockShockMine}地雷 可以复原地板(深水?)
    * ${Iconc.blockImpulsePump}水泵(大)可以开扩大片地图
""".trimIndent()
)

var myTiles = emptyList<IslandTile>()
val waterFloor by autoInit {
    state.rules.tags.get("@waterFloor")
        ?.let { content.getByName(ContentType.block, it) as? Floor }
        ?: Blocks.deepwater.asFloor()!!
}

inner class IslandTile(val tile: Tile) {
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
                discoverQueue.add(myTiles[tile.array()])
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

val blocksToOpen = mapOf(
    Blocks.mechanicalPump to 1,
    Blocks.rotaryPump to 3,
    Blocks.impulsePump to 5,
)

onEnable {
    myTiles = world.tiles.map(::IslandTile)
    Groups.build.filter { it.block in blocksToOpen }.forEach {
        it.tile.setAir()
        it.tile.circle(blocksToOpen[it.block]!!) { x, y ->
            myTiles[world.packArray(x, y)].discoverInit()
        }
    }
}

onDisable {
    discoverQueue.clear()
    myTiles = emptyList()
}

listen<EventType.BlockBuildEndEvent> {
    if (it.breaking) return@listen
    val tile = it.tile
    when (val block = tile.block()) {
        in blocksToOpen -> {
            var any = false
            tile.getLinkedTiles { if (myTiles[it.array()].discover()) any = true }
            if (any) {
                launch(Dispatchers.game) {
                    delay(100)
                    Call.deconstructFinish(tile, block, null)
                }
                tile.circle(blocksToOpen[block]!!) { x, y ->
                    discoverQueue.add(myTiles[world.packArray(x, y)])
                }
            }
        }

        Blocks.shockMine ->
            if (myTiles[tile.array()].unDiscover())
                launch(Dispatchers.game) {
                    delay(100)
                    Call.deconstructFinish(tile, block, null)
                }
    }
}

val discoverQueue = mutableListOf<IslandTile>()
listen(Trigger.update) {
    repeat(5) {
        val tile = discoverQueue.removeFirstOrNull() ?: return@listen
        tile.discover()
    }
}