package wayzer.user.ext

import arc.util.io.Writes
import cf.wayzer.placehold.PlaceHoldApi.with
import coreLibrary.lib.CommandContext
import coreLibrary.lib.CommandHandler
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.Commands
import coreMindustry.lib.ClientOnly
import coreMindustry.lib.broadcast
import coreMindustry.lib.player
import mindustry.Vars
import mindustry.gen.Building
import mindustry.gen.Call
import mindustry.gen.Player
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Duration


object SkillPrecheck : CommandHandler {
    private val mapDisabled get() = Vars.state.rules.tags.getBool("@noSkills")

    context(CommandContext) override suspend fun handle() {
        ClientOnly.handle()
        if (mapDisabled) returnReply("[red]当前地图禁用技能".with())
        if (player!!.dead()) returnReply("[red]死亡状态无法使用技能".with())
    }
}

object SkillNoPvp : CommandHandler {
    context(CommandContext) override suspend fun handle() {
        if (Vars.state.rules.pvp) returnReply("[red]当前技能PVP模式禁用".with())
    }
}

/** 技能冷却
 * @param coolDown in ms, -1一局冷却
 * */
class SkillCooldown(val coolDown: Int = -1) : CommandHandler {
    private val lastUsed = mutableMapOf<String, Long>()

    init {
        SkillCommands.allCooldown.add(this)
    }

    context(CommandContext) override suspend fun handle() {
        if (!checkCoolDown()) CommandInfo.Return()
    }

    context(CommandContext) fun checkCoolDown(): Boolean {
        val key = player!!.uuid()
        if (key in lastUsed) {
            if (coolDown < 0) {
                reply("[red]该技能每局限用一次".with())
                return false
            } else if (lastUsed[key]!! + coolDown >= System.currentTimeMillis()) {
                reply(
                    "[red]技能冷却，还剩{time 秒}".with(
                        "time" to Duration.ofMillis(lastUsed[key]!! + coolDown - System.currentTimeMillis())
                    )
                )
                return false
            }
        }
        return true
    }

    context(CommandContext) fun setCoolDown() {
        val key = player!!.uuid()
        lastUsed[key] = System.currentTimeMillis()
    }

    fun reset() = lastUsed.clear()
}

@Suppress("unused")
object SkillCommands : Commands() {
    val allCooldown = mutableListOf<SkillCooldown>()
    @Suppress("MemberVisibilityCanBePrivate")
    class SkillScope(val player: Player) {
        fun broadcastSkill(skill: String) = broadcast(
            "[yellow][技能][green]{player.name}[white]使用了[green]{skill}[white]技能."
                .with("player" to player, "skill" to skill), quite = true
        )

        //util

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
}

@CommandInfo.CommandBuilder
fun CommandInfo.skillBody(body: suspend context(CommandContext) SkillCommands.SkillScope.() -> Unit) {
    body {
        body.invoke(context, SkillCommands.SkillScope(player!!))
        attrs.filterIsInstance<SkillCooldown>().singleOrNull()?.setCoolDown()
    }
}