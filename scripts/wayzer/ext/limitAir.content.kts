import mindustry.content.Blocks
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups

val enable get() = state.rules.tags.getBool("@limitAir")

onEnable {
    launch {
        while (true) {
            delay(3_000)
            if (net.server() && enable) {
                Groups.unit.forEach {
                    if (it.type().flying && state.teams.closestEnemyCore(it.x, it.y, it.team)?.within(it, state.rules.enemyCoreBuildRadius) == true) {
                        it.player?.sendMessage("[red]该地图限制空军,禁止进入敌方领空".with())
                        it.kill()
                    }
                }
            }
        }
    }
}

listen<EventType.BlockBuildEndEvent> {
    if (it.tile.block() == Blocks.airFactory && !it.breaking) {
        if (enable)
            Call.label("[yellow]本地图限制空军,禁止进入敌方领空", 60 * 60f, it.tile.getX(), it.tile.getY())
    }
}