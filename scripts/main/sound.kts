package main

import arc.audio.Sound
import mindustry.gen.Call
import mindustry.gen.Sounds

val map by lazy {
    Sounds::class.java.fields.filter { it.type == Sound::class.java}
        .associate { it.name to (it.get(null) as Sound) }
}

command("sound", "发出声音") {
    usage = "[-a 全体可见] [类型=列出] [响度=1] [音高=0] ([声向=0](-1 左 0 双 1 右) / [-p 坐标模式] [x轴数值=玩家坐标] [y轴数值=玩家坐标])"
    permission = id.replace("/", ".")
    body {
        val all = checkArg("-a")
        val type = arg.getOrNull(0)?.let { map[it] }
            ?: returnReply("[red]请输入类型: {list}".with("list" to map.keys))
        val arg1 = arg.getOrNull(1)?.toFloatOrNull() ?: 1f
        val arg2 = arg.getOrNull(2)?.toFloatOrNull() ?: 1f
        val arg3 = arg.getOrNull(3)?.toFloatOrNull() ?: 1f
        val pos = checkArg("-p")
        if (pos) {
            val x = arg.getOrNull(4)?.toFloatOrNull() ?: player!!.x
            val y = arg.getOrNull(5)?.toFloatOrNull() ?: player!!.y
            if (all) Call.soundAt(type, x, y, arg1, arg2)
            else Call.soundAt(player!!.con, type, x, y, arg1, arg2)
        }
        if (all) Call.sound(type, arg1, arg2, arg3)
        else Call.sound(player!!.con, type, arg1, arg2, arg3)
    }
}