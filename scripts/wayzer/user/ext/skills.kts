package wayzer.user.ext


listen<EventType.ResetEvent> {
    SkillCommands.allCooldown.forEach { it.reset() }
}
command("skill", "技能菜单") {
    aliases = listOf("技能")
    attr(SkillPrecheck)
    body(SkillCommands)
}

command("mono", "技能: 召唤采矿机,一局限一次,PVP禁用".with(), commands = SkillCommands) {
    aliases = listOf("矿机")
    attr(SkillPrecheck)
    attr(SkillNoPvp)
    attr(SkillCooldown())
    requirePermission("wayzer.user.skills.mono")
    skillBody {
        UnitTypes.mono.create(player.team()).also {
            it.set(player)
        }.add()
        broadcastSkill("采矿机?")
    }
}