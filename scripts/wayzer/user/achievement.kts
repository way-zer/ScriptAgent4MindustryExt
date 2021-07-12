@file:Depends("wayzer/user/userService")

package wayzer.user

import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.Achievement as DB

val userService = contextScript<UserService>()

fun finishAchievement(profile: PlayerProfile, name: String, exp: Int, broadcast: Boolean = false) {
    transaction {
        if (!DB.newWithCheck(profile.id, name, exp)) return@transaction
        userService.updateExp(profile, exp, "完成成就")
        userService.notify(
            profile,
            "[gold][成就]恭喜{player.name}[gold]完成成就[scarlet]{name},[gold]获得[violet]{exp}[gold]经验",
            mapOf("name" to name, "exp" to exp.toString(), if (broadcast) ("_" to "") else "player.name" to "你"),
            broadcast
        )
    }
}
export(::finishAchievement)

command("achieve", "管理指令: 添加成就") {
    this.usage = "<qq> <name> <exp>"
    this.type = CommandType.Server
    permission = "wayzer.user.achieve"
    body {
        if (arg.size < 3) replyUsage()
        val profile = arg[0].toLongOrNull()?.let {
            PlayerProfile.findByQQ(it)
        } ?: returnReply("[red]找不到该用户".with())
        val name = arg[1]
        val exp = arg[2].toIntOrNull() ?: returnReply("[red]请输入正确的数字".with())
        finishAchievement(profile, name, exp, false)
        reply("[green]添加成功".with())
    }
}