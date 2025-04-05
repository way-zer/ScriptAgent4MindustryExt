package wayzer.pvp

import mindustry.game.Team
import mindustry.type.Item
import mindustry.type.UnitType
import mindustry.world.Block
import mindustry.world.blocks.production.Drill
import mindustry.world.blocks.production.GenericCrafter
import mindustry.world.blocks.storage.CoreBlock

val specialBuild = arrayOf(
    Blocks.graphitePress, Blocks.multiPress,
    Blocks.siliconSmelter, Blocks.siliconCrucible,
    Blocks.plastaniumCompressor, Blocks.phaseWeaver, Blocks.surgeSmelter,
    Blocks.thermalGenerator, Blocks.foreshadow
)

class TeamData {
    val ores = mutableSetOf<String>()//drill+ore
    val unit = mutableSetOf<UnitType>()
    val build = mutableSetOf<Block>()
}

val data = arrayOfNulls<TeamData>(Team.all.size)
listen<EventType.ResetEvent> {
    data.fill(null)
}
val Team.myData
    get() = data[id] ?: TeamData().also {
        data[id] = it
    }

fun log(team: Team, msg: PlaceHoldString) {
    if (state.rules.fog) return
    broadcast(
        "{team.colorizeName}[]{msg}".with("team" to team, "msg" to msg),
        MsgType.InfoToast,
        quite = true
    )
}

listen<EventType.BlockBuildEndEvent> {
    if (!state.rules.pvp || it.breaking) return@listen
    fun produce(block: Block, item: Item) {
        log(it.team, "开始使用 {block} 生产 {item}".with("block" to block, "item" to item))
    }

    val data = it.team.myData
    val block = it.tile.block()
    if (block is Drill) {
        val item = (it.tile.build as Drill.DrillBuild).dominantItem
        if (data.ores.add("${block.name}-${item.name}")) {
            produce(block, item)
        }
    }
    if (block in specialBuild && data.build.add(block)) {
        if (block is GenericCrafter) {
            val item = block.outputItem.item
            produce(block, item)
        } else {
            log(it.team, "开始建筑 {block}".with("block" to block))
        }
    }
}

listen<EventType.UnitCreateEvent> {
    if (!state.rules.pvp || it.unit.spawnedByCore) return@listen
    val team = it.unit.team
    val type = it.unit.type
    if (team.myData.unit.add(type)) {
        log(team, "完成生产单位 {unit}".with("unit" to type))
    }
}

listen<EventType.BlockDestroyEvent> { e ->
    if (!state.rules.pvp || e.tile.block() !is CoreBlock) return@listen
    val team = e.tile.team()
    if (team.data().cores.none { it != e.tile.build }) {
        log(team, "淘汰".with())
    }
}