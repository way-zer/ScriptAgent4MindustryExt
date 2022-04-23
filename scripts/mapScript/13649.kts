@file:Depends("coreMindustry/utilMapRule", "修改核心单位,单位属性")
@file:Depends("wayzer/map/betterTeam", "更改玩家队伍")

package mapScript

import arc.Events
import arc.util.Align
import arc.util.Time
import cf.wayzer.scriptAgent.define.annotations.Depends
import coreLibrary.lib.util.loop
import mindustry.content.Blocks
import mindustry.content.StatusEffects
import mindustry.content.UnitTypes
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.world.blocks.storage.CoreBlock
import kotlin.math.ceil
import wayzer.map.BetterTeam

val betterTeam = contextScript<BetterTeam>()

/**@author xkldklp
 * https://mdt.wayzer.top/v2/map/13649/latest
 */
name = "废墟之城-感染"

fun Player.toCrux(){
    betterTeam.changeTeam(this,Team.crux)
    broadcast("{name}[red]被机械干扰波及".with("name" to name), quite = true)
    sendMessage("[cyan]你已被机械干扰影响,被转化为红队\n" +
            "[red]现在，消灭你曾经的队友")
}

listen<EventType.UnitDestroyEvent> {
    val player = it.unit.player
    if (player == null || player.team() == Team.crux) return@listen
    player.toCrux()
}

onEnable {
    contextScript<coreMindustry.UtilMapRule>().apply {
        registerMapRule((Blocks.coreShard as CoreBlock)::unitType) { UnitTypes.nova }
        registerMapRule((Blocks.coreFoundation as CoreBlock)::unitType) { UnitTypes.pulsar }
        registerMapRule((Blocks.coreNucleus as CoreBlock)::unitType) { UnitTypes.quasar }
        registerMapRule(UnitTypes.nova::health) { 1000f }
        registerMapRule(UnitTypes.nova.weapons.get(0).bullet::healPercent) { 0f }
        registerMapRule(UnitTypes.nova.weapons.get(0).bullet::damage) { 48f }
        registerMapRule(UnitTypes.pulsar::health) { 1500f }
        registerMapRule(UnitTypes.pulsar.weapons.get(0).bullet::healPercent) { 0f }
        registerMapRule(UnitTypes.pulsar.weapons.get(0).bullet::damage) { 36f }
        registerMapRule(UnitTypes.pulsar.weapons.get(0).bullet::status) { StatusEffects.electrified }
        registerMapRule(UnitTypes.pulsar.weapons.get(0).bullet::statusDuration) { 2f * 60f }
        registerMapRule(UnitTypes.quasar::health) { 600f }
        registerMapRule(UnitTypes.quasar.weapons.get(0).bullet::healPercent) { 0f }
        registerMapRule(UnitTypes.quasar.weapons.get(0).bullet::damage) { 20f }
        registerMapRule(UnitTypes.vela::health) { 2000f }
        registerMapRule(UnitTypes.dagger::health) { 150f }
        registerMapRule(UnitTypes.dagger.weapons.get(0).bullet::damage) { 35f }// *5
        registerMapRule(UnitTypes.mace::health) { 150f }
        registerMapRule(UnitTypes.mace.weapons.get(0).bullet::damage) { 50f }// *5
        registerMapRule(UnitTypes.fortress::health) { 150f }
        registerMapRule(UnitTypes.fortress.weapons.get(0).bullet::damage) { 60f }// *5
        registerMapRule(UnitTypes.spiroct::health) { 150f }
        registerMapRule(UnitTypes.spiroct.weapons.get(0).bullet::damage) { 0f }
        registerMapRule(UnitTypes.spiroct.weapons.get(0).bullet::status) { StatusEffects.slow }
        registerMapRule(UnitTypes.spiroct.weapons.get(0).bullet::statusDuration) { 3f * 60f }
        registerMapRule(UnitTypes.spiroct.weapons.get(2).bullet::damage) { 0f }
        registerMapRule(UnitTypes.spiroct.weapons.get(2).bullet::status) { StatusEffects.disarmed }
        registerMapRule(UnitTypes.spiroct.weapons.get(2).bullet::statusDuration) { 0.4f * 60f }
    }
    val startTime = Time.millis()
    launch(Dispatchers.game){
        var leftTime = 120
        while (leftTime > 0) {
            broadcast("[yellow]机械干扰暂未波及(预测被波及时间：{time}秒)".with("time" to leftTime), quite = true)
            delay(30_000)
            leftTime -= 30
        }
        val count = ceil(Groups.player.size() * 0.2).toInt()
        Groups.player.shuffled().take(count).forEach {
            it.toCrux()
        }
        broadcast("[yellow]机械干扰已经到达！！！".with(), quite = true)
        state.rules.pvp = true
    }
    val needTime = state.rules.tags.getInt("@needTime", 800)
    loop(Dispatchers.game) {
        delay(2000)
        Call.infoPopup(
            "幸存单位：${Team.sharded.data().unitCount}\n" +
                    "已被干扰单位：${Team.crux.data().unitCount}\n" +
                    "距离机械干扰消散还剩：${needTime - Time.timeSinceMillis(startTime) / 1000f} 秒", 2.013f,
            Align.topLeft, 350, 0, 0, 0
        )
    }
    launch(Dispatchers.game) {
        delay(30_000)//开局单位可能未出生
        broadcast("[yellow]幸存者模块启用 幸存单位少于玩家数量*0.2时获得强力buff".with(), quite = true)
        while (Time.timeSinceMillis(startTime) / 1000f < needTime) {
            delay(5_000)
            if (Team.sharded.data().unitCount <= ceil(Groups.player.size() * 0.2)) {
                Team.sharded.data().units.each {
                    it.apply {
                        apply(StatusEffects.overdrive, 999999f)
                        apply(StatusEffects.overclock, 999999f)
                        apply(StatusEffects.boss, 999999f)
                    }
                }
            }
            if (Team.sharded.data().unitCount == 0) {
                Call.announce("全部单位确认死亡")
                state.gameOver = true
                Events.fire(EventType.GameOverEvent(Team.crux))
                return@launch
            }
        }

        Team.crux.data().units.each { it.kill() }
        Call.announce("机械干扰已经结束")
        state.gameOver = true
        Events.fire(EventType.GameOverEvent(Team.sharded))
    }
}

