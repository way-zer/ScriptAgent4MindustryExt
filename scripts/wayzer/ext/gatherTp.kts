package wayzer.ext

import arc.math.geom.Vec2
import mindustry.core.World
import mindustry.entities.Units
import mindustry.gen.Unit
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

fun check(unit: Unit, x: Float, y: Float): Boolean {
    unit.solidity()?.let {
        if (it.solid(World.toTile(x), World.toTile(y)))
            return false
    }
    if (!unit.type.flying && Units.count(x, y, unit.physicSize()) {
            it.isGrounded && it.type.allowLegStep == unit.type.allowLegStep
        } > 0)
        return false
    return true
}
listen<EventType.PlayerChatEvent> {
    if (it.message.equals("go", true) && lastPos != Vec2.ZERO) {
        val unit = it.player.unit()
        var i = 0
        launch(Dispatchers.game) {
            while (!unit.within(lastPos, 8 * 5f) && i < 10) {
                i++
                if (!check(unit, lastPos.x, lastPos.y)) {
                    it.player.sendMessage("[yellow]目标位置无法安全传送")
                    return@launch
                }
                unit.set(lastPos.x, lastPos.y)
                delay(1)
            }
            it.player.sendMessage("[red]传送失败,请重试")
        }
    }
}

listen<EventType.ResetEvent> {
    lastPos = Vec2.ZERO
}