package mapScript.tags

import coreLibrary.lib.util.loop

registerMapTag("@limitAir")

onEnable {
    loop(Dispatchers.game) {
        delay(3_000)
        Groups.unit.forEach {
            if (it.type().flying && it.closestEnemyCore()?.within(it, state.rules.enemyCoreBuildRadius) == true) {
                it.player?.sendMessage("[red]该地图限制空军,禁止进入敌方领空".with())
                it.kill()
            }
        }
    }
}

listen<EventType.BlockBuildEndEvent> {
    if (it.tile.block() == Blocks.airFactory && !it.breaking) {
        Call.label("[yellow]本地图限制空军,禁止进入敌方领空", 60f, it.tile.getX(), it.tile.getY())
    }
}

listen<EventType.PlayerJoin> {
    it.player.sendMessage("[yellow]本地图限制空军,禁止进入敌方领空")
}