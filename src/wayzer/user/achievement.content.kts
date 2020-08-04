package wayzer.user

import cf.wayzer.script_agent.IContentScript
import cf.wayzer.script_agent.depends
import mindustry.entities.type.Player
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.reflect.KCallable

fun finishAchievement(p: Player, name: String, exp: Int, broadcast: Boolean = false) {
    PlayerData[p.uuid].apply {
        if (profile == null) return
        @Suppress("UNCHECKED_CAST")
        val updateExp = depends("wayzer/user/level").let { it as? IContentScript }?.import<KCallable<*>>("updateExp") as? Player.(Int) -> Boolean
        if (updateExp?.invoke(p, exp) == true) {
            @Suppress("EXPERIMENTAL_API_USAGE")
            val result = transaction {
                Achievement.newWithCheck(profile!!.id,name,exp)
            }
            if(!result)return
            if (broadcast) {
                broadcast("[gold][成就]恭喜{player.name}[gold]完成成就[scarlet]{name},[gold]获得[violet]{exp}[gold]经验".with(
                        "player" to p, "name" to name, "exp" to exp
                ))
            } else {
                p.sendMessage("[gold][成就]恭喜你完成成就[scarlet]{name},[gold]获得[violet]{exp}[gold]经验".with(
                        "name" to name, "exp" to exp
                ))
            }
        } else {
            println("[Error]等级系统不可用")
        }
    }
}
export(::finishAchievement)

command("achieve", "管理指令: 添加成就", "<id> <name> <exp>", CommandType.Server) { arg, p ->
    if (arg.size < 3) return@command p.sendMessage("[red]参数不足 <id> <name> <exp>")
    val player = arg[0].let { id -> playerGroup.singleOrNull { it.uuid.startsWith(id) } }
            ?: return@command p.sendMessage("[red]找不到玩家")
    val name = arg[1]
    val exp = arg[2].toIntOrNull() ?: return@command p.sendMessage("[red]请输入正确的数字")
    finishAchievement(player, name, exp, false)
    p.sendMessage("[green]添加成功")
}