package coreMindustry

import coreLibrary.lib.util.ReceivedEvent
import coreLibrary.lib.util.nextEvent
import mindustry.gen.SendChatMessageCallPacket

data class OnChat(val player: Player, val text: String) : Event, ReceivedEvent {
    override var received: Boolean = false

    companion object : Event.Handler()
}

listenPacket2ServerAsync<SendChatMessageCallPacket> { con, p ->
    con.player?.let { OnChat(it, p.message).emitAsync().received.not() } ?: true
}

suspend fun nextChat(player: Player, timeoutMillis: Int): String? = withTimeoutOrNull(timeoutMillis.toLong()) {
    nextEvent<OnChat> { it.player == player }.text
}
export(::nextChat)