package wayzer.ext
//WayZer 版权所有(请勿删除版权注解)
import arc.graphics.Color
import mindustry.entities.Effect

name = "扩展功能: 显示粒子效果"

val map by lazy {
    Fx::class.java.fields.filter { it.type == Effect::class.java }
        .associate { it.name to (it.get(null) as Effect) }
}

command("effect", "显示效果") {
    usage = "[-a 全体可见] [类型=列出] [半径=10] [颜色=red]"
    permission = dotId
    body {
        val all = checkArg("-a")
        val type = arg.getOrNull(0)?.let { map[it] }
            ?: returnReply("[red]请输入类型: {list}".with("list" to map.keys))
        val arg1 = arg.getOrNull(1)?.toFloatOrNull() ?: 10f
        val arg2 = arg.getOrNull(2)?.let { Color.valueOf(it) } ?: Color.red
        if (all) Call.effect(type, player!!.x, player!!.y, arg1, arg2)
        else Call.effect(player!!.con, type, player!!.x, player!!.y, arg1, arg2)
    }
}