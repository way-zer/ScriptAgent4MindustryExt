package mapScript.tags

import mindustry.game.EventType.Trigger
import mindustry.game.Team
import mindustry.type.Item
import mindustry.world.blocks.storage.CoreBlock
import org.intellij.lang.annotations.Language

registerMapTag("@autoExchange")
modeIntroduce(
    "等价交换", """
    创意/实现：[gold]WayZer[]
    所有资源自动进行等价交换，按比例保持同步，核心资源为当前最大可兑换数量
    [green]TIP[]: 可能有暴富的错觉，建议只看单种资源判断价值
    
    所有核心容量=原版*10
    0值资源(BAN)：沙，休眠胞，裂变物质
    1值资源：绝大部分
    2值资源：钛，石墨，硅，硫
    3值资源：钍，塑料，氧化物，爆炸化合物
    4值资源：钨，碳化物，布，合金
""".trimIndent()
)

val cores = content?.run {
    blocks().filterIsInstance<CoreBlock>().map {
        """block.${it.name}.itemCapacity: ${it.itemCapacity * 10},"""
    }
}.orEmpty()
@Language("JSON5")
val patch = """
{
    "name": "AutoExchange",
    ${cores.joinToString("\n")}
}
""".trimIndent()
mapPatches = listOf(patch)

val score = IntArray(Team.all.size)
onEnableForGame {
    score.fill(0)
    state.teams.getActive().forEach {
        score[it.team.id] = it.team.items().get(Items.copper)
    }
}
onDisable {
    score.fill(0)
}
val Item.score: Int
    get() = when (this) {
        Items.sand, Items.fissileMatter, Items.dormantCyst -> 0
        Items.titanium, Items.graphite, Items.silicon, Items.pyratite -> 2
        Items.thorium, Items.plastanium, Items.oxide, Items.blastCompound -> 3
        Items.tungsten, Items.carbide, Items.phaseFabric, Items.surgeAlloy -> 4
        else -> 1
    }

fun Int.toCount(item: Item): Int = if (item.score == 0) 0 else (this / item.score).coerceAtLeast(0)
fun Int.toScore(item: Item): Int = this * item.score
fun syncItems(team: Team) {
    val old = score[team.id]
    val items = team.data().core()?.items ?: return
    val delta = content.items().sumOf { item ->
        (items.get(item) - old.toCount(item)).toScore(item)
    }
    val new = old + delta
    score[team.id] = new
    content.items().forEach {
        if (it.score != 0)
            items.set(it, new.toCount(it))
    }
}

listen(Trigger.update) {
    Team.all.forEach {
        if (it.data().noCores()) score[it.id] = 0
        else syncItems(it)
    }
}

command("debugScore", "".asPlaceHoldString()) {
    body {
        reply(score.toList().toString().asPlaceHoldString())
    }
}