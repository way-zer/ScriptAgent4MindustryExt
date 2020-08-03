package wayzer.ext

import mindustry.game.EventType

val type by config.key(MsgType.InfoMessage, "发送方式")
val template by config.key("""
    Welcome to this Server
    [green]欢迎{player.name}[green]来到本服务器[]
""".trimIndent(), "欢迎信息模板")

listen<EventType.PlayerJoin> {
    it.player.sendMessage(template.with(),type)
}