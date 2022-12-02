@file:Depends("coreMindustry/utilNext")
@file:Depends("wayzer/user/ext/skills")
@file:Depends("wayzer/user/ext/skillsC")

package wayzer.user.ext

import coreLibrary.lib.ConfigBuilder.Companion.configs
import coreLibrary.lib.PlaceHold.registeredVars
import coreLibrary.lib.config
import coreMindustry.lib.*
import coreMindustry.lib.Listener.Companion.listener
import coreMindustry.lib.RootCommands.getSubCommands
import wayzer.lib.dao.PlayerData
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

val perPage by config.key(8, "快速施法每页命令个数")

command("skills", "快速施法") {
    usage = "[page]"
    type = CommandType.Client
    aliases = listOf("技能")
    body {
        val context = this
        var page = context.arg.firstOrNull()?.toIntOrNull() ?: 1
        val player = player!!
        val id = PlayerData[player.uuid()].profile?.id?.value ?: 0
        fun sendMenu() {
            launch(Dispatchers.game) {
                contextScript<coreMindustry.UtilNext>().sendMenuBuilder<Unit>(player, 30_000, "快速施法", "Page $page / ${
                    ceil(getSubCommands(context).values.toSet().filter {
                        (it.permission.isBlank() || context.hasPermission(it.permission)) && it.permission.startsWith(
                            "wayzer.user.skill"
                        )
                    }.size * 1f / perPage).toInt()
                }") {
                    var count = 1
                    getSubCommands(context).values.toSet().filter {
                        (it.permission.isBlank() || context.hasPermission(it.permission)) && it.permission.startsWith(
                            "wayzer.user.skill"
                        )
                    }.forEach {
                        count++
                        if (count - 1 !in perPage * (page - 1)..perPage * page) return@forEach
                        val key = "${it.name}@${it.aliases}@$id"
                        add(
                            listOf(
                                "${if (check(key)) "[green]" else "[red]"}${it.name} [acid]${it.aliases}\n [sky]${it.description}${
                                    if (!check(key)) checkText(
                                        key
                                    ) else ""
                                }" to {
                                    it.invoke(context)
                                })
                        )
                    }
                    add(listOf(
                        "<-" to {
                            page = max(page - 1, 1)
                            sendMenu()
                        },
                        "$page / ${
                            ceil(getSubCommands(context).values.toSet().filter {
                                (it.permission.isBlank() || context.hasPermission(it.permission)) && it.permission.startsWith(
                                    "wayzer.user.skill"
                                )
                            }.size * 1f / perPage).toInt()
                        }" to { sendMenu() },
                        "->" to {
                            page = min(page + 1, ceil(getSubCommands(context).values.toSet().filter {
                                (it.permission.isBlank() || context.hasPermission(it.permission)) && it.permission.startsWith(
                                    "wayzer.user.skill"
                                )
                            }.size * 1f / perPage).toInt())
                            sendMenu()
                        }
                    ))
                    add(listOf("取消" to { }))
                }
            }
        }
        sendMenu()
    }
}

fun check(key: String): Boolean{
    if (Skills.Api.script.used[key] != null){
        return Skills.Api.script.used[key]!! < System.currentTimeMillis()
    }
    if(SkillsC.Api.script.used[key] != null){
        return SkillsC.Api.script.used[key]!! < System.currentTimeMillis()
    }
    return true
}

fun checkText(key: String): String{
    if (Skills.Api.script.used[key] != null){
        return "[red]" + if(Skills.Api.script.used[key]!! >= System.currentTimeMillis()) ((Skills.Api.script.used[key]!! - System.currentTimeMillis()) / 1000).toString() + "s" else "无法使用"
    }
    if(SkillsC.Api.script.used[key] != null){
        return "[red]" + if(SkillsC.Api.script.used[key]!! >= System.currentTimeMillis()) ((SkillsC.Api.script.used[key]!! - System.currentTimeMillis()) / 1000).toString() + "s" else "无法使用"
    }
    return ""
}