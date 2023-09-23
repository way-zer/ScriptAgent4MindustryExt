package wayzer.admin

import arc.Events
import mindustry.core.GameState
import mindustry.game.Team
import mindustry.net.Packets
import kotlin.system.exitProcess

var msg: String? = null
var doRestart: () -> Unit = {}
listen<EventType.GameOverEvent> {
    val msg = msg ?: return@listen
    broadcast("[yellow]服务器即将重启: {msg}".with("msg" to msg), quite = true)
}
listen<EventType.PlayerJoin> {
    val msg = msg ?: return@listen
    launch(Dispatchers.gamePost) {
        it.player.sendMessage("[yellow]服务器将在本局游戏后自动重启: {msg}".with("msg" to msg))
    }
}
//Don't using ResetEvent, as Groups.player is cleared
//listen<EventType.ResetEvent>
listen<EventType.StateChangeEvent> {
    if (it.to == GameState.State.menu)
        doRestart()
}

fun scheduleRestart(msg: String, beforeExit: () -> Unit = {}) {
    this.msg = msg
    broadcast("[yellow]服务器将在本局游戏后自动重启: {msg}".with("msg" to msg))
    doRestart = {
        broadcast("[yellow]服务器重启:\n{msg}".with("msg" to msg), MsgType.InfoMessage)
        Thread.sleep(1000L)
        Groups.player.forEach {
            it.kick(Packets.KickReason.serverRestarting)
        }
        Thread.sleep(100L)
        beforeExit()
        exitProcess(2)
    }
    if (state.isMenu)
        Core.app.post(doRestart)
}

command("restart", "计划重启") {
    usage = "[--now] <msg>"
    permission = dotId
    body {
        val now = checkArg("--now")
        val msg = arg.joinToString(" ")
        scheduleRestart(msg)
        if (now) {
            Events.fire(EventType.GameOverEvent(Team.derelict))
            doRestart()
        }
    }
}