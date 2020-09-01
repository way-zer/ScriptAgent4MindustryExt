package wayzer.user

import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.game.Gamemode
import java.time.Duration

val used by PlaceHold.reference<MutableMap<String, Long>>("_skills_used_set")
registerVar("_skills_used_set", "内部使用:技能系统已使用集合", runCatching { used }.getOrElse { mutableMapOf() })

listen<EventType.ResetEvent> {
    used.clear()
}

fun skill(name: String, desc: String, vararg aliases: String, body: CommandContext.() -> Unit) {
    command(name, desc, {
        permission = "wayzer.user.skills.$name"
        this.aliases = aliases.toList()
        type = CommandType.Client
    }, body)
}

/**
 * @param coolDown in ms; <0 ->一局冷却
 */
fun CommandContext.checkBefore(allowPvp: Boolean, coolDown: Int?) {
    if (state.rules.mode() !in arrayOf(Gamemode.survival, Gamemode.attack) && (!allowPvp || state.rules.pvp)) {
        reply("[red]当前模式禁用".with())
        CommandInfo.Return()
    }
    if (player!!.dead) {
        reply("[red]你已死亡".with())
        CommandInfo.Return()
    }
    if (coolDown != null) {
        val id = PlayerData[player!!.uuid].profile?.id?.value ?: 0
        val key = "${thisCommand.name}@$id"
        if (key in used) {
            if (coolDown < 0) {
                reply("[red]该技能每局限用一次".with())
                CommandInfo.Return()
            } else if (used[key]!! >= System.currentTimeMillis()) {
                reply("[red]技能冷却，还剩{time:秒}".with("time" to Duration.ofMillis(used[key]!! - System.currentTimeMillis())))
                CommandInfo.Return()
            }
        }
        used[key] = System.currentTimeMillis() + coolDown
    }
}

fun Player.broadcastSkill(skill: String) = broadcast("[yellow][技能][green]{player.name}[white]使用了[green]{skill}[white]技能.".with("player" to this, "skill" to skill), quite = true)

skill("draug", "技能: 召唤采矿机,一局限一次,PVP禁用", "矿机") {
    checkBefore(false, -1)
    if (state.rules.bannedBlocks.contains(Blocks.draugFactory))
        return@skill reply("[red]该地图采矿机已禁封,禁止召唤".with())
    UnitTypes.draug.create(player!!.team).apply {
        set(player!!.x, player!!.y)
        add()
    }
    player!!.broadcastSkill("召唤采矿机")
}

skill("noFire", "技能: 灭火,半径10格,冷却120s", "灭火") {
    checkBefore(false, 120_000)
    fireGroup.forEach {
        if (it.dst(player!!) <= 80)
            it.remove()
    }
    player!!.broadcastSkill("灭火")
}