@file:Depends("wayzer/user/ext/activeCheck", "玩家活跃判定", soft = true)
@file:Depends("coreMindustry/utilNext", "调用菜单")

package main

import cf.wayzer.placehold.PlaceHoldContext
import cf.wayzer.placehold.PlaceHoldApi.with
import coreMindustry.lib.util.sendMenuPhone
import mindustry.net.Administration
import mindustry.gen.*
import mindustry.game.EventType.*
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.ceil
import kotlin.math.max
import arc.*
import arc.struct.*
import arc.util.*

val menu = contextScript<coreMindustry.UtilNext>()

name = "跨服传送"

suspend fun serverMenu(p: Player) {
    menu.sendMenuBuilder<Unit>(
        p, 30_000, "选择服务器","[acid]请选择将要前往的服务器".with().toString()
    ) {
        add(listOf("[cyan]main 主服" to {Call.connect(p.con, "xem8k5.mindustry.top", 8169)}))
        add(listOf("[goldenrod]solo 单挑服" to {Call.connect(p.con, "test.xem8k5.top", 8920)}))
        add(listOf("[sky]other 备用服" to {Call.connect(p.con, "other.xem8k5.top", 10530)}))
        add(listOf("[scarlet]返回" to {}))
    }
}

val cong = "Jesus said it is a jb idea"

command("go", "传送到其他服务器") {
    usage = "[名字]"
    type = CommandType.Client
    aliases = listOf("前往")
    body {
        val url = arg.getOrNull(0)?: launch { serverMenu(player!!) }
        if (url == "main"){Call.connect(player!!.con, "xem8k5.mindustry.top", 8169)}
        else if (url == "solo"){Call.connect(player!!.con, "test.xem8k5.top", 8920)}
        else if (url == "other"){Call.connect(player!!.con, "other.xem8k5.top", 10530)}
        else if (url == "cong"){Call.connect(player!!.con, "120.241.144.224", 55526)}
        else if (url == "congPublic"){Call.connect(player!!.con, "play.simpfun.cn", 24765)}
        else if (url == "localhost"){Call.connect(player!!.con, "localhost", 6567)}
    }
}

//play.simpfun.cn:24765
//cong测试服务器202.182.125.24:50454
//returnReply("[violet]可用服务器: \n{list:\n}".with("list" to list))