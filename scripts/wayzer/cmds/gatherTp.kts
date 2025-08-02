@file:Depends("wayzer/user/ext/skills", "Gather也算技能")

package wayzer.cmds

import cf.wayzer.placehold.PlaceHoldApi.with
import mindustry.entities.Units
import mindustry.gen.Unit
import mindustry.world.Tile
import wayzer.user.ext.SkillCooldown
import wayzer.user.ext.SkillNoPvp
import wayzer.user.ext.SkillPrecheck
import wayzer.user.ext.skillBody
import java.time.Duration
import java.time.Instant

PermissionApi.registerDefault("wayzer.ext.gather")

var lastPos: Tile? = null
var lastTime: Instant = Instant.MIN

command("gather", "发出集合请求") {
    usage = "[可选说明]"
    aliases = listOf("集合")
    attr(SkillPrecheck)
    attr(SkillNoPvp)
    attr(SkillCooldown(30_000))
    requirePermission("wayzer.ext.gather")
    skillBody {
        if (player.dead() || !player.unit().type.targetable)
            returnReply("[red]当前单位无法使用 集合".with())
        if (Duration.between(lastTime, Instant.now()) < Duration.ofSeconds(10)) {
            returnReply("[red]刚刚有人发起请求,请稍等10s再试".with())
        }
        val message = "[white]\"${arg.firstOrNull() ?: ""}[white]\""
        val tile = player.tileOn() ?: returnReply("[red]请在地图内使用".with())
        lastPos = tile
        lastTime = Instant.now()
        broadcastSkill("集合(${tile.x},${tile.y})")
        broadcast("可输入\"[gold]go[white]\"前往：{message}".with("message" to message), quite = true)
    }
}

command("tp", "传送到鼠标坐标") {
    attr(ClientOnly)
    requirePermission("wayzer.ext.tp")
    body {
        val player = player!!
        player.unit()?.apply {
            set(player.mouseX, player.mouseY)
            snapInterpolation()
        }
    }
}

fun check(unit: Unit, tile: Tile): Boolean {
    if (unit.type.flying) return true
    return unit.canPass(tile.x.toInt(), tile.y.toInt()) &&
            Units.count(tile.worldx(), tile.worldy(), unit.physicSize()) { it.isGrounded && it.hitSize > 14.0F } > 0
}
listen<EventType.PlayerChatEvent> {
    val tile = lastPos ?: return@listen
    if (it.message.equals("go", true)) {
        it.player.unit()?.apply {
            if (!check(this, tile)) {
                it.player.sendMessage("[yellow]目标位置无法安全传送")
                return@listen
            }
            set(tile)
            snapInterpolation()
        }
    }
}

listen<EventType.ResetEvent> {
    lastPos = null
}