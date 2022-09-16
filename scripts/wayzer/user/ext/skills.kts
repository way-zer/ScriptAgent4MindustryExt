package wayzer.user.ext

import arc.util.io.Writes
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.gen.Building
import wayzer.user.ext.Skills.Api.skill
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Duration

@Savable(false)
val used = mutableMapOf<String, Long>()
customLoad(::used, used::putAll)
listen<EventType.ResetEvent> { used.clear() }

@Suppress("unused")
companion object Api {
    lateinit var script: Skills
    private val used get() = script.used

    @DslMarker
    annotation class SkillScopeMarker

    @Suppress("MemberVisibilityCanBePrivate")
    class SkillScope(val name: String, val player: Player, val ctx: CommandContext) {
        @SkillScopeMarker
        fun returnReply(msg: PlaceHoldString): Nothing = ctx.returnReply(msg)

        @SkillScopeMarker
        fun checkNotPvp() {
            if (state.rules.pvp)
                returnReply("[red]当前模式禁用".with())
        }

        /** @param coolDown in ms,  -1一局冷却 */
        fun checkCoolDown(coolDown: Int, set: Boolean = true): Boolean {
            val id = PlayerData[player.uuid()].profile?.id?.value ?: 0
            val key = "${name}@$id"
            if (key in used) {
                if (coolDown < 0) {
                    ctx.reply("[red]该技能每局限用一次".with())
                    return false
                } else if (used[key]!! >= System.currentTimeMillis()) {
                    ctx.reply("[red]技能冷却，还剩{time:秒}".with("time" to Duration.ofMillis(used[key]!! - System.currentTimeMillis())))
                    return false
                }
            }
            if (set) used[key] = System.currentTimeMillis() + coolDown
            return true
        }

        /** @param coolDown in ms,  -1一局冷却 */
        @SkillScopeMarker
        fun checkOrSetCoolDown(coolDown: Int) {
            if (!checkCoolDown(coolDown)) CommandInfo.Return()
        }

        @SkillScopeMarker
        fun broadcastSkill(skill: String) = broadcast(
            "[yellow][技能][green]{player.name}[white]使用了[green]{skill}[white]技能."
                .with("player" to player, "skill" to skill), quite = true
        )
    }

    @ScriptDsl
    fun Script.skill(name: String, desc: String, vararg aliases: String, body: SkillScope.() -> Unit) {
        command(name, desc) {
            permission = "wayzer.user.skills.$name"
            this.aliases = aliases.toList()
            type = CommandType.Client
            body {
                @Suppress("MemberVisibilityCanBePrivate")
                if (player!!.dead())
                    returnReply("[red]你已死亡".with())
                SkillScope(name, player!!, this).body()
            }
        }
    }

    fun syncTile(vararg builds: Building) {
        val outStream = ByteArrayOutputStream()
        val write = DataOutputStream(outStream)
        builds.forEach {
            write.writeInt(it.pos())
            write.writeShort(it.block.id.toInt())
            it.writeAll(Writes.get(write))
        }
        Call.blockSnapshot(builds.size.toShort(), outStream.toByteArray())
    }
}
Api.script = this

skill("mono", "技能: 召唤采矿机,一局限一次,PVP禁用", "矿机") {
    if (state.rules.bannedBlocks.contains(Blocks.airFactory))
        returnReply("[red]该地图采矿机已禁封,禁止召唤".with())
    checkNotPvp()
    checkOrSetCoolDown(-1)
    val unit = player.unit()
    UnitTypes.mono.create(player.team()).apply {
        set(unit.x, unit.y)
        add()
    }
    broadcastSkill("采矿机?")
}
