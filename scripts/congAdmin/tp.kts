/*
@file:Depends("wayzer/user/ext/activeCheck", "玩家活跃判定", soft = true)
@file:Depends("coreMindustry/utilNext", "调用菜单")

package main

import mindustry.*
import arc.*
import arc.math.geom.Vec2
import cf.wayzer.placehold.PlaceHoldApi.with
import coreMindustry.lib.*

command("tp", "传送") {
    usage = "[url]"
    permission = "cong.admin.tp"
    body {
        val pos: Vec2 = Vec2.ZERO
        val x = arg.getOrNull(0)?.toIntOrNull()?: returnReply("[scarlet]请输入x".with())
        val y = arg.getOrNull(1)?.toIntOrNull()?: returnReply("[scarlet]请输入y".with())
        val unit = player!!.unit()
        unit.set(pos.x, pos.y)
        broadcast("即将传送{player}到{x} {y}".with("player" to player!!.name , "x" to pos.x.toInt() / 8, "y" to pos.y.toInt() / 8))
        returnReply("[acid]即将传送至 {x} {y}".with("x" to pos.x.toInt() / 8, "y" to pos.y.toInt() / 8))
    }
}
*/
@file:Depends("wayzer/user/ext/activeCheck", "玩家活跃判定", soft = true)
@file:Depends("coreMindustry/utilNext", "调用菜单")

package main

import mindustry.*
import arc.*
import arc.math.geom.Vec2
import cf.wayzer.placehold.PlaceHoldApi.with
import coreMindustry.lib.*

command("tp", "传送") {
    usage = "[url]"
    permission = "cong.admin.tp"
    body {
        val x = arg.getOrNull(0)?.toIntOrNull()?: returnReply("[scarlet]请输入x".with())
        val y = arg.getOrNull(1)?.toIntOrNull()?: returnReply("[scarlet]请输入y".with())
        val unit = player!!.unit()
        unit.set(x.toFloat(), y.toFloat())
        broadcast("即将传送{player}到{x} {y}".with("player" to player!!.name , "x" to x , "y" to y))
        returnReply("[acid]即将传送至 {x} {y}".with("x" to x , "y" to y))
    }
}
