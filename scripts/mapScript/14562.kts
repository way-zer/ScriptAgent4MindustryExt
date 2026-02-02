package mapScript

import mindustry.ctype.ContentType
import mindustry.game.EventType.Trigger
import mindustry.gen.Iconc
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


val blocksToOpen = mapOf(
    Blocks.mechanicalPump to 1,
    Blocks.rotaryPump to 3,
    Blocks.impulsePump to 5,
)

onEnable {
    IslandTile.waterFloor = state.rules.tags.get("@waterFloor")
        ?.let { content.getByName(ContentType.block, it) as? Floor }
        ?: Blocks.deepwater.asFloor()!!
    IslandTile.tiles = world.tiles.map(::IslandTile)
    Groups.build.filter { it.block in blocksToOpen }.forEach {
        it.tile.setAir()
        it.tile.circle(blocksToOpen[it.block]!!) { x, y ->
            IslandTile.tiles[world.packArray(x, y)].discoverInit()
        }
    }
}

onDisable {
    IslandTile.discoverQueue.clear()
    IslandTile.tiles = emptyList()
}

listen<EventType.BlockBuildEndEvent> {
    if (it.breaking) return@listen
    val tile = it.tile
    when (val block = tile.block()) {
        in blocksToOpen -> {
            var any = false
            tile.getLinkedTiles { if (IslandTile.tiles[it.array()].discover()) any = true }
            if (any) {
                launch(Dispatchers.game) {
                    delay(100)
                    Call.deconstructFinish(tile, block, null)
                }
                tile.circle(blocksToOpen[block]!!) { x, y ->
                    IslandTile.discoverQueue.add(IslandTile.tiles[world.packArray(x, y)])
                }
            }
        }

        Blocks.shockMine ->
            if (IslandTile.tiles[tile.array()].unDiscover())
                launch(Dispatchers.game) {
                    delay(100)
                    Call.deconstructFinish(tile, block, null)
                }
    }
}

listen(Trigger.update) {
    repeat(5) {
        val tile = IslandTile.discoverQueue.removeFirstOrNull() ?: return@listen
        tile.discover()
    }
}