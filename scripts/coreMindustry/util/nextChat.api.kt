package coreMindustry.util

import cf.wayzer.scriptAgent.Event
import cf.wayzer.scriptAgent.thisContextScript
import coreLibrary.lib.util.ReceivedEvent
import coreLibrary.lib.util.nextEvent
import kotlinx.coroutines.withTimeoutOrNull
import mindustry.gen.Player

data class OnChat(val player: Player, val text: String) : Event, ReceivedEvent {
    override var received: Boolean = false

    companion object : Event.Handler()
}

suspend fun nextChat(player: Player, timeoutMillis: Int): String? = withTimeoutOrNull(timeoutMillis.toLong()) {
    thisContextScript().nextEvent<OnChat> { it.player == player }.text
}