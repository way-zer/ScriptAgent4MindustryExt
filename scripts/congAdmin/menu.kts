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

name = "菜单"

val menu = contextScript<coreMindustry.UtilNext>()

val Player.active
    get() = textFadeTime >= 0 || depends("wayzer/user/ext/activeCheck")
        ?.import<(Player) -> Int>("inactiveTime")
        ?.let { it(this) < 30_000 } ?: true


lateinit var canReceive: (Player) -> Boolean
fun MP() = Groups.player.filter(canReceive)

command("menu", "发送菜单") {
    usage = "[标题][正文][按钮数量][选项1][选项2][选项3][选项4][选项5][选项6]"
    aliases = listOf("sendmenu")
    permission = "cong.admin.menu"
    body {
        val Title = arg.getOrNull(0)?: returnReply("[scarlet]请输入标题".with())
        val Text = arg.getOrNull(1)?: returnReply("[scarlet]请输入正文".with())
        val Type = arg.getOrNull(2)?.toIntOrNull()?: returnReply("[scarlet]请输入按钮数量".with())
        val Choose1 = arg.getOrNull(3)?: returnReply("[scarlet]请输入选项1名字".with())
        val Choose2 = arg.getOrNull(4)?: ""
        val Choose3 = arg.getOrNull(5)?: ""
        val Choose4 = arg.getOrNull(6)?: ""
        val Choose5 = arg.getOrNull(7)?: ""
        val Choose6 = arg.getOrNull(8)?: ""
        suspend fun sendMenu1(p: Player) {
            menu.sendMenuBuilder<Unit>(
                p, 30_000, Title,Text.with().toString()
            ) {
                add(listOf(Choose1 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose1))}))
            }
        }
        suspend fun sendMenu2(p: Player) {
            menu.sendMenuBuilder<Unit>(
                p, 30_000, Title,Text.with().toString()
            ) {
                add(listOf(
                    Choose1 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose1))},
                    Choose2 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose2))}
                ))
            }
        }
        suspend fun sendMenu3(p: Player) {
            menu.sendMenuBuilder<Unit>(
                p, 30_000, Title,Text.with().toString()
            ) {
                add(listOf(
                    Choose1 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose1))},
                    Choose2 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose2))}
                ))
                add(listOf(Choose3 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose3))}))
            }
        }
        suspend fun sendMenu4(p: Player) {
            menu.sendMenuBuilder<Unit>(
                p, 30_000, Title,Text.with().toString()
            ) {
                add(listOf(
                    Choose1 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose1))},
                    Choose2 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose2))}
                ))
                add(listOf(
                    Choose3 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose3))},
                    Choose4 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose4))}
                ))
            }
        }
        suspend fun sendMenu5(p: Player) {
            menu.sendMenuBuilder<Unit>(
                p, 30_000, Title,Text.with().toString()
            ) {
                add(listOf(
                    Choose1 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose1))}
                ))
                add(listOf(
                    Choose2 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose3))},
                    Choose3 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose4))}
                ))
                add(listOf(
                    Choose4 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose4))},
                    Choose5 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose5))}
                ))
            }
        }
        suspend fun sendMenu6(p: Player) {
            menu.sendMenuBuilder<Unit>(
                p, 30_000, Title,Text.with().toString()
            ) {
                add(listOf(
                    Choose1 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose1))},
                    Choose2 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose2))},
                    Choose3 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose3))}
                ))
                add(listOf(
                    Choose4 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose4))},
                    Choose5 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose5))},
                    Choose6 to {broadcast("{player} [sky]选了选项[] {choose}".with("player" to p.name,"choose" to Choose6))}
                ))
            }
        }
        val Titles = Title+" "+Text
        if (Type <= 1) {
            val Menu1Text = "$Choose1"
            MP().forEach{
                launch{sendMenu1(it)}
                Call.infoPopup(it.con, "{player}[sky]发起了菜单[]".with("player" to player!!.name).toString(), 2.013f, Align.left, if (mobile) -520 else -505, 0, 0, 0)
                Call.infoPopup(it.con, Titles.with().toString(), 2.013f, Align.left, if (mobile) -470 else -455, 0, 0, 0)
                Call.infoPopup(it.con, Menu1Text.with().toString(), 2.013f, Align.left, if (mobile) -420 else -405, 0, 0, 0)
            }
            player.sendMessage("[acid]菜单生成完成")
            broadcast("====================".with())
            broadcast("{player}[sky]发起了菜单[]".with("player" to player!!.name))
            broadcast(Titles.with())
            broadcast(Menu1Text.with())
            broadcast("====================".with())

        }
        else if (Type <= 2) {
            val Menu2Text = "$Choose1   $Choose2"
            MP().forEach{
                launch{sendMenu2(it)}
                Call.infoPopup(it.con, "{player}[sky]发起了菜单[]".with("player" to player!!.name).toString(), 2.013f, Align.left, if (mobile) -520 else -505, 0, 0, 0)
                Call.infoPopup(it.con, Titles.with().toString(), 2.013f, Align.left, if (mobile) -470 else -455, 0, 0, 0)
                Call.infoPopup(it.con, Menu2Text.with().toString(), 2.013f, Align.left, if (mobile) -420 else -405, 0, 0, 0)
            }
            player.sendMessage("[acid]菜单生成完成")
            broadcast("====================".with())
            broadcast("{player}[sky]发起了菜单[]".with("player" to player!!.name))
            broadcast(Titles.with())
            broadcast(Menu2Text.with())
            broadcast("====================".with())

        }
        else if (Type <= 3) {
            val Menu3Text = "$Choose1   $Choose2   $Choose3"
            MP().forEach{
                launch{sendMenu3(it)}
                Call.infoPopup(it.con, "{player}[sky]发起了菜单[]".with("player" to player!!.name).toString(), 2.013f, Align.left, if (mobile) -520 else -505, 0, 0, 0)
                Call.infoPopup(it.con, Titles.with().toString(), 2.013f, Align.left, if (mobile) -470 else -455, 0, 0, 0)
                Call.infoPopup(it.con, Menu3Text.with().toString(), 2.013f, Align.left, if (mobile) -420 else -405, 0, 0, 0)
            }
            player.sendMessage("[acid]菜单生成完成")
            broadcast("====================".with())
            broadcast("{player}[sky]发起了菜单[]".with("player" to player!!.name))
            broadcast(Titles.with())
            broadcast(Menu3Text.with())
            broadcast("====================".with())
        }
        else if (Type <= 4) {
            val Menu4Text = "$Choose1   $Choose2   $Choose3"
            val Menu4Text2 = "$Choose4"
            MP().forEach{
                launch{sendMenu4(it)}
                Call.infoPopup(it.con, "{player}[sky]发起了菜单[]".with("player" to player!!.name).toString(), 2.013f, Align.left, if (mobile) -520 else -505, 0, 0, 0)
                Call.infoPopup(it.con, Titles.with().toString(), 2.013f, Align.left, if (mobile) -470 else -455, 0, 0, 0)
                Call.infoPopup(it.con, Menu4Text.with().toString(), 2.013f, Align.left, if (mobile) -420 else -405, 0, 0, 0)
                Call.infoPopup(it.con, Menu4Text2.with().toString(), 2.013f, Align.left, if (mobile) -370 else -355, 0, 0, 0)
            }
            player.sendMessage("[acid]菜单生成完成")
            broadcast("====================".with())
            broadcast("{player}[sky]发起了菜单[]".with("player" to player!!.name))
            broadcast(Titles.with())
            broadcast(Menu4Text.with())
            broadcast(Menu4Text2.with())
            broadcast("====================".with())
        }
        else if (Type <= 5) {
            val Menu5Text = "$Choose1"
            val Menu5Text2 = "$Choose2   $Choose3"
            val Menu5Text3 = "$Choose4   $Choose5"
            MP().forEach{
                launch{sendMenu5(it)}
                Call.infoPopup(it.con, "{player}[sky]发起了菜单[]".with("player" to player!!.name).toString(), 2.013f, Align.left, if (mobile) -520 else -505, 0, 0, 0)
                Call.infoPopup(it.con, Titles.with().toString(), 2.013f, Align.left, if (mobile) -470 else -455, 0, 0, 0)
                Call.infoPopup(it.con, Menu5Text.with().toString(), 2.013f, Align.left, if (mobile) -420 else -405, 0, 0, 0)
                Call.infoPopup(it.con, Menu5Text2.with().toString(), 2.013f, Align.left, if (mobile) -370 else -355, 0, 0, 0)
                Call.infoPopup(it.con, Menu5Text3.with().toString(), 2.013f, Align.left, if (mobile) -320 else -305, 0, 0, 0)
            }
            player.sendMessage("[acid]菜单生成完成")
            broadcast("====================".with())
            broadcast("{player}[sky]发起了菜单[]".with("player" to player!!.name))
            broadcast(Titles.with())
            broadcast(Menu5Text.with())
            broadcast(Menu5Text2.with())
            broadcast(Menu5Text3.with())
            broadcast("====================".with())
        }
        else if (Type <= 6) {
            val Menu6Text = "$Choose1   $Choose2   $Choose3"
            val Menu6Text2 = "$Choose4   $Choose5   $Choose6"
            MP().forEach{
                launch{sendMenu6(it)}
                Call.infoPopup(it.con, "{player}[sky]发起了菜单[]".with("player" to player!!.name).toString(), 2.013f, Align.left, if (mobile) -520 else -505, 0, 0, 0)
                Call.infoPopup(it.con, Titles.with().toString(), 2.013f, Align.left, if (mobile) -470 else -455, 0, 0, 0)
                Call.infoPopup(it.con, Menu6Text.with().toString(), 2.013f, Align.left, if (mobile) -420 else -405, 0, 0, 0)
                Call.infoPopup(it.con, Menu6Text2.with().toString(), 2.013f, Align.left, if (mobile) -370 else -355, 0, 0, 0)
            }
            player.sendMessage("[acid]菜单生成完成")
            broadcast("====================".with())
            broadcast("{player}[sky]发起了菜单[]".with("player" to player!!.name))
            broadcast(Titles.with())
            broadcast(Menu6Text.with())
            broadcast(Menu6Text2.with())
            broadcast("====================".with())
        }
        else player.sendMessage("[scarlet]菜单不支持")

        //player.sendMessage(Title+" "+Text+" "+Choose1+" "+Choose2)
    }
}
fun reset() {canReceive = { !it.dead() && it.active }}
onEnable { reset() }

//Call.infoPopup(it.con, right.with().toString(), 2.013f, Align.left, if (mobile) -370 else -355, 0, 0, 0)
//registerVar("scoreBroad.ext.contentsVersion", "ContentsTweaker状态显示", DynamicVar.v {"[violet]特殊修改已加载: [orange](使用[sky]ContentsTweaker[]MOD获得最佳体验)".takeIf { patches != null }})
