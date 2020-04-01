package main
//WayZer 版权所有(请勿删除版权注解)
import mindustry.ctype.ContentType
import mindustry.game.Team
import mindustry.type.UnitType

name="扩展功能: 召唤单位"

command("spawn", "召唤单位", "[类型ID=列出] [队伍ID,默认为sharded] [数量=1]") { arg, player ->
    if (player?.isAdmin == false)//限制管理员或者后台使用
        return@command player.sendMessage("[red]你没有权限使用该命令")
    val list = content.getBy<UnitType>(ContentType.unit)
    val type = arg.getOrNull(0)?.toIntOrNull()?.let { list.items.getOrNull(it) } ?: return@command player.sendMessage(
            "[red]请输入类型ID: ${list.mapIndexed { i, type -> "[yellow]$i[green]($type)" }.joinToString()}"
    )
    val team = arg.getOrNull(1)?.let { s->
        s.toIntOrNull()?.let { Team.all().getOrNull(it) }?:return@command player.sendMessage(
                "[red]请输入队伍ID: ${Team.base().mapIndexed { i, type -> "[yellow]$i[green]($type)" }.joinToString()}"
        )
    }?: Team.sharded
    val num = arg.getOrNull(2)?.toIntOrNull()?:1
    (1..num).forEach { _ ->
        type.create(team).apply {
            if(player!=null)set(player.x,player.y)
            else team.data().core()?.let {
                    set(it.x,it.y)
                }
            add()
        }
    }
    player.sendMessage("[green]成功为 $team 生成 $num 只 ${type.name}")
}