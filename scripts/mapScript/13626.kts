package mapScript

import arc.Events
import arc.util.Align
import arc.util.Interval
import coreLibrary.lib.util.loop
import mindustry.content.Blocks
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.game.Team
import mindustry.gen.Groups
import kotlin.random.Random

/**@author xkldklp
 * https://mdt.wayzer.top/v2/map/13626/latest
 */
name = "昼夜行动P.M.6:00"
//实际上 这是一个套着pvp外壳的boss战


var minutes = 360
var stage = 1
val interval = Interval(2)

suspend fun loopUtilKill() {
    while (Team.get(8).data().unitCount > 0) {
        if (interval[0, 180 * 60f]) {
            Call.announce(
                "[red]检测到时间波动！\n" +
                        "世界时间被快进了！"
            )
            minutes += Random.nextInt(1, 60)
        }
        if (interval[1, 150 * 60f]) {
            Call.announce("[red]核心发生异常！资源丢失！")
            Team.sharded.data().core().items.clear()
        }

        val text = buildString {
            appendLine("[cyan]STAGE II")
            Team.get(8).data().units.sortedBy { it.health }.forEach {
                appendLine("[red]${it.type}: [green]\uE813[white]${it.health}")
            }
            appendLine("[yellow]\uE88B[white]${180 - interval.getTime(0) / 60f}")
            appendLine("[red]\uE819[white]${150 - interval.getTime(1) / 60f}")
        }
        Call.infoPopup(
            text, 2.013f,
            Align.topLeft, 380, 0, 0, 0
        )
        delay(2000L)
    }
}

onEnable {
    loop(Dispatchers.game) {
        delay(2000)
        val hour = (minutes.toFloat() / 60).toInt()
        val minute = minutes % 60
        Call.infoPopup(
            "当前时间PM$hour:$minute", 2.013f,
            Align.topLeft, 280, 0, 0, 0
        )
    }
    loop(Dispatchers.game) {
        delay(7_500)
        minutes += 1//时间流速480:1
        if (minutes >= stage * 240 + 300) {//阶段1将在9:00触发 阶段2后0:00
            minutes = 0
            Call.announce("STORY END")
            delay(5000)
            Call.announce("结局D:永夜降临")
            delay(2000)
            state.gameOver = true
            Events.fire(EventType.GameOverEvent(Team.get(8)))
            Team.sharded.data().destroyToDerelict()
        }
    }
}

listen<EventType.CoreChangeEvent> { event ->
    val team = event.core.team
    val lastDamage = event.core.lastDamage
    launch(Dispatchers.gamePost) {
        if (team.data().hasCore() || stage > 1) return@launch
        stage++
        when {
            team == Team.sharded -> {
                Call.announce("STORY END")
                delay(5000)
                Call.announce(
                    "结局B:永夜\n" +
                            "[red]昼夜.. 不是敌人"
                )
                delay(2000)
                state.gameOver = true
                Events.fire(EventType.GameOverEvent(Team.get(8)))
                team.data().destroyToDerelict()
            }
            team == Team.blue -> {
                Call.announce("STORY END")
                delay(5000)
                Call.announce(
                    "结局A:极昼\n" +
                            "[red]昼夜.. 不是敌人"
                )
                delay(2000)
                state.gameOver = true
                Events.fire(EventType.GameOverEvent(Team.get(8)))
                team.data().destroyToDerelict()
            }
            team == Team.get(8) && lastDamage != Team.blue -> {
                Call.announce("STORY END")
                delay(5000)
                Call.announce(
                    "结局C:极昼..?\n" +
                            "[red]破局的关机 在于夜"
                )
                delay(2000)
                state.gameOver = true
                Events.fire(EventType.GameOverEvent(Team.get(8)))
                Team.blue.data().destroyToDerelict()
            }
            team == Team.get(8) && lastDamage == Team.blue -> {
                Call.announce("[red]STORY START")
                delay(5000)
                Call.announce("[gray]为什么？")
                delay(2000)
                Call.announce("[gray]你们..为什么要这么做？")
                delay(2000)
                Call.announce("[gray]既然如此..")
                delay(3000)
                Call.announce("[gray]那就去死！")
                Team.blue.data().destroyToDerelict()
                delay(5000)
                Call.announce(
                    "节点E:昼夜行动完成 开始隐藏任务\n" +
                            "[blue]蓝队已被摧毁 请/ob至黄队"
                )
                Groups.player.filter { it.team() == Team.blue }.forEach {
                    it.team(Team.sharded)
                    it.clearUnit()
                }
                delay(5000)
                Call.announce(
                    "永夜降临时间已被调整到0:00\n" +
                            "[black]终末神域-[gray]混沌领主 再临"
                )
                UnitTypes.reign.spawn(Team.get(8), 333 * 8f, 146 * 8f).apply {
                    apply(StatusEffects.boss, 999999f)
                    apply(StatusEffects.overclock, 999999f)
                    apply(StatusEffects.overdrive, 999999f)
                    apply(StatusEffects.slow, 999999f)
                    maxHealth = 2400000f
                    health = 2400000f
                    mounts += UnitTypes.omura.create(Team.get(8)).mounts()
                }
                delay(3000)
                world.tile(150, 275).setNet(Blocks.coreNucleus, Team.sharded, 0)
                world.tile(335, 265).setNet(Blocks.coreNucleus, Team.sharded, 0)
                world.tile(140, 30).setNet(Blocks.coreNucleus, Team.sharded, 0)
                world.tile(380, 30).setNet(Blocks.coreNucleus, Team.sharded, 0)

                launch(Dispatchers.game) {
                    while (Team.sharded.data().hasCore()) yield()
                    Call.announce("STORY END")
                    delay(5000)
                    Call.announce("终局F:技输一筹")
                    delay(2000)
                    state.gameOver = true
                    Events.fire(EventType.GameOverEvent(Team.get(8)))
                    Team.blue.data().destroyToDerelict()
                }

                loopUtilKill()

                Call.announce("STORY END")
                delay(5000)
                Call.announce("终局G:永不磨灭")
                state.gameOver = true
                Events.fire(EventType.GameOverEvent(Team.sharded))
            }
        }
    }
}

