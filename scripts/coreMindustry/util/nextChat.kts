package coreMindustry.util

import mindustry.gen.SendChatMessageCallPacket

listenPacket2ServerAsync<SendChatMessageCallPacket> { con, p ->
    con.player?.let { OnChat(it, p.message).emitAsync().received.not() } ?: true
}
export(::nextChat)