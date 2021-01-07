package wayzer.user

import org.jetbrains.exposed.sql.transactions.transaction
import wayzer.lib.dao.Achievement

fun finishAchievement(profile: PlayerProfile, name: String, exp: Int, broadcast: Boolean = false) {
    val updateExp = depends("wayzer/user/level")?.import<PlayerProfile.(Int) -> List<Player>>("updateExp")
    if (updateExp == null) {
        println("[Error]等级系统不可用")
        return
    }
    @OptIn(CacheEntity.NeedTransaction::class)
    transaction {
        if (!Achievement.newWithCheck(profile.id, name, exp)) return@transaction
        val players = profile.updateExp(exp)
        profile.save()
        if (broadcast) {
            broadcast("[gold][成就]恭喜{player.name}[gold]完成成就[scarlet]{name},[gold]获得[violet]{exp}[gold]经验".with(
                    "player" to (players.firstOrNull() ?: ""), "name" to name, "exp" to exp
            ))
        } else {
            players.forEach {
                it.sendMessage("[gold][成就]恭喜你完成成就[scarlet]{name},[gold]获得[violet]{exp}[gold]经验".with(
                        "name" to name, "exp" to exp
                ))
            }
        }
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
            transaction {
                @OptIn(CacheEntity.NeedTransaction::class)
                PlayerProfile.getOrFindByQQ(it, false)
            }
        } ?: returnReply("[red]找不到该用户".with())
        val name = arg[1]
        val exp = arg[2].toIntOrNull() ?: returnReply("[red]请输入正确的数字".with())
        finishAchievement(profile, name, exp, false)
        reply("[green]添加成功".with())
    }
}