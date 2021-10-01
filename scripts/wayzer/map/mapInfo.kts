package wayzer.map

import mindustry.gen.Groups
import mindustry.world.blocks.storage.CoreBlock


fun CoreBlock.CoreBuild.showInfo(p: Player) {
    if (p.con == null) return
    var i = 0
    val desc = state.map.description().map {
        if (i > 20 && it in charArrayOf(' ', '，', ',', '.', '。', '!', '！')) {
            i = 0
            '\n'
        } else {
            i++
            it
        }
    }.joinToString("")
    Call.label(
        p.con, """
        |[white]${state.map.name()}
        |
        |[purple]By: [scarlet]${state.map.author()}
        |[white]${desc}
    """.trimMargin(), 2 * 60f, x, y
    )
}

listen<EventType.PlayEvent> {
    launch {
        delay(5000)
        launch(Dispatchers.game) {
            Groups.player.forEach {
                it.team().cores().firstOrNull()?.showInfo(it)
            }
        }
    }
}

listen<EventType.PlayerJoin> { e ->
    Core.app.post {
        e.player.team().cores().firstOrNull()?.showInfo(e.player)
    }
}