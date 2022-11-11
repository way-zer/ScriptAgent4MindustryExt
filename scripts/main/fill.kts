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
import arc.math.geom.Vec2
import coreMindustry.lib.command
import coreMindustry.lib.player
import mindustry.Vars.world
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.ctype.ContentType
import mindustry.game.Team
import mindustry.type.*


command("fill", "填充") {
    usage = "[x1][y1][x2][y2][block]"
    permission = "cong.admin.fill"
    body {
        val x1 = arg.getOrNull(0)?.toIntOrNull()?: returnReply("[scarlet]请输入x1".with())
        val y1 = arg.getOrNull(1)?.toIntOrNull()?: returnReply("[scarlet]请输入y1".with())
        val x2 = arg.getOrNull(2)?.toIntOrNull()?: returnReply("[scarlet]请输入x2".with())
        val y2 = arg.getOrNull(3)?.toIntOrNull()?: returnReply("[scarlet]请输入y2".with())
/*
        val list = content.getBy<Blocks>(ContentType.block)
        val type = arg.getOrNull(4)?.toIntOrNull()?.let { list.items.getOrNull(it) } ?: returnReply(
            "[red]请输入类型ID: {list}"
                .with("list" to list.mapIndexed { i, type -> "[yellow]$i[green]($type)" }.joinToString())
        )
*/


        for(x in x1 until x2){
            for(y in y1 until y2){

                val tile = world.tiles.get(x, y)

                world.tiles.getc(x, y).apply {
                    if (block() == Blocks.air){
                        tile?.setNet(Blocks.itemSource, player!!.team(), 0)
                    }
                }

            }
        }

        returnReply("[acid]即将填充至 {x1},{y1}-{x2},{y2}".with("x1" to x1 , "y1" to y1 , "x2" to x2 , "y2" to y2))

    }
}

/*
skill("era", "技能: 生成一个7x7的反应装甲，冷却200秒", "反应装甲") {
    checkOrSetCoolDown(200000)
    checkNotPvp()

    val unit = player.unit()

    for(x in -3..3){
        for(y in -3..3){

            val tile = world.tiles.get(
                unit.tileX() + x,
                unit.tileY() + y
            )

            world.tiles.getc(unit.tileX() + x, unit.tileY() + y).apply {
                if (block() == Blocks.air){
                    tile?.setNet(Blocks.thoriumWall, player.team(), 0)
                }
            }

        }
    }

    broadcastSkill("反应装甲")
}