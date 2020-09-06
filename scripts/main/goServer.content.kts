package main
//WayZer 版权所有(请勿删除版权注解)
import mindustry.gen.Call

name = "跨服传送"

val servers by config.key(mapOf<String, String>(), "服务器传送列表", "格式: {名字: \"介绍;地址\"} (;作为分割符)")

data class Info(val name: String, val desc: String, val address: String, val port: Int)

val infos: Map<String, Info>
    get() = servers.mapValues { (k, v) ->
        val sp1 = v.split(";")
        assert(sp1.size == 2) { "格式错误: $v" }
        val sp2 = sp1[1].split(":")
        val port = sp2.getOrNull(1)?.toIntOrNull() ?: port
        Info(k, sp1[0], sp2[0], port)
    }


command("go", "传送到其他服务器", {
    usage = "[名字,为空列出]";
    type = CommandType.Client
    aliases = listOf("前往")
}) {
    val info = arg.firstOrNull()
            ?.let { infos[it] ?: return@command reply("[red]错误的服务器名字".with()) }
            ?: let {
                val list = infos.values.map { "[gold]{name}:[tan]{desc}\n".with("name" to it.name, "desc" to it.desc) }
                return@command reply("[violet]可用服务器: \n{list}".with("list" to list))
            }
    Call.onConnect(player!!.con, info.address, info.port)
    broadcast("[cyan][-][salmon]{player.name}[salmon]传送到了{name}服务器(/go {name})".with("player" to player!!, "name" to info.name))
}