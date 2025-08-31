package coreMindustry
//WayZer 版权所有(请勿删除版权注解)
import arc.util.Align
import java.time.Duration

val defaultTemplate = """
[sky]欢迎 {cV}{player.name} [sky]
{cK}当前地图: {cV}[{map.id}]{map.name}
{cK}游戏时间: {cV}{state.gameTime 分钟}
{listPrefix scoreboard.ext|joinLines}
{listPrefix scoreBroad.ext|joinLines}
{cA}输入 /broad 可以开关该显示
""".trimIndent()

val template by config.key(
    defaultTemplate, "积分榜模板",
    "其中{cK}{cV}{cA}为颜色变量，{listPrefix xx}行供其他插件动态扩展"
)
//Color变量 cK - KEY, cV - VALUE, cA - ACTION
val msg get() = template.with("cK" to "[gray]", "cV" to "[lightgray]", "cA" to "[slate]")

val disabled = mutableSetOf<String>()

command("board", "开关积分板显示") {
    aliases = listOf("broad", "scoreboard")
    attr(ClientOnly)
    body {
        if (!disabled.remove(player!!.uuid()))
            disabled.add(player!!.uuid())
        reply("[green]切换成功".with())
    }
}

//避免找不到 scoreboard.ext.* 变量
registerVar("scoreboard.ext.null", "空占位", null)
registerVar("scoreBroad.ext.null", "空占位(兼容旧插件)", null)

onEnable {
    loop(Dispatchers.game) {
        delay(Duration.ofSeconds(2).toMillis())
        Groups.player.forEach {
            if (disabled.contains(it.uuid())) return@forEach
            val mobile = it.con?.mobile == true
            Call.infoPopup(
                it.con, msg.with().toPlayer(it), 2.013f,
                Align.topLeft, if (mobile) 210 else 155, 0, 0, 0
            )
        }
    }
}