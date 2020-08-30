package wayzer.user

import mindustry.content.UnitTypes
import mindustry.entities.type.Player
import mindustry.game.EventType
import mindustry.game.Gamemode

val used by PlaceHold.reference<MutableSet<String>>("_skills_used_set")
registerVar("_skills_used_set", "内部使用:技能系统已使用集合", runCatching { used }.getOrElse { mutableSetOf() })

fun skill(name: String, desc: String, vararg aliases: String, body: CommandContext.() -> Unit) {
    command(name, desc, {
        permission = "wayzer.user.skills.$name"
        this.aliases = aliases.toList()
        type = CommandType.Client
    }, body)
}

fun CommandContext.checkBefore(allowPvp: Boolean, useId: String?) {
    if (state.rules.mode() !in arrayOf(Gamemode.survival, Gamemode.attack) && (!allowPvp || state.rules.pvp)) {
        reply("[red]当前模式禁用".with())
        CommandInfo.Return()
    }
    if (player!!.dead) {
        reply("[red]你已死亡".with())
        CommandInfo.Return()
    }
    if (useId != null) {
        val id = PlayerData[player!!.uuid].profile?.id?.value ?: 0
        val key = "${useId}@$id"
        if (key in used) {
            reply("[red]你已使用该技能".with())
            CommandInfo.Return()
        } else used.add(key)
    }
}

fun Player.broadcastSkill(skill: String) = broadcast("[yellow][技能][green]{player.name}[white]使用了[green]{skill}[white]技能.".with("player" to this, "skill" to skill), quite = true)

skill("draug", "技能: 召唤采矿机,一局限一次,PVP禁用", "矿机") {
    checkBefore(false, "矿机")
    repeat(2) {
        UnitTypes.draug.create(player!!.team).apply {
            set(player!!.x, player!!.y)
            add()
        }
    }
    player!!.broadcastSkill("召唤采矿机")
}

listen<EventType.ResetEvent> {
    used.clear()
}