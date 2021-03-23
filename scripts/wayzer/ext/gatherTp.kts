package wayzer.ext

import arc.math.geom.Vec2
import mindustry.gen.Call
import java.time.Duration
import java.time.Instant

PermissionApi.registerDefault("wayzer.ext.gather")

var lastPos: Vec2 = Vec2.ZERO
var lastTime: Instant = Instant.MIN

command("gather", "发出集合请求") {
    usage = "[可选说明]"
    type = CommandType.Client
    aliases = listOf("集合")
    permission = "wayzer.ext.gather"
    body {
        if (Duration.between(lastTime, Instant.now()) < Duration.ofSeconds(30)) {
            returnReply("[red]刚刚有人发起请求,请稍等30s再试".with())
        }
        val message = "[white]\"${arg.firstOrNull() ?: ""}[white]\""
        lastPos = player!!.unit().run { Vec2(lastX, lastY) }
        lastTime = Instant.now()
        broadcast(
            "[yellow][集合][cyan]{player.name}[white]发起集合([red]{x},{y}[white]){message},输入\"[gold]go[white]\"前往"
                .with(
                    "player" to player!!,
                    "x" to lastPos.x.toInt() / 8,
                    "y" to lastPos.y.toInt() / 8,
                    "message" to message
                ), quite = true
        )
    }
}

listen<EventType.PlayerChatEvent> {
    if (it.message.equals("go", true) && lastPos != Vec2.ZERO) {
        it.player.unit().set(lastPos.x, lastPos.y)
        it.player.set(lastPos.x, lastPos.y)
        Call.setPosition(it.player.con, lastPos.x, lastPos.y)
    }
}

listen<EventType.ResetEvent> {
    lastPos = Vec2.ZERO
}