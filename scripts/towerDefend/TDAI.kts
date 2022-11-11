@file:Depends("private/towerDefend/TDControl")

package private.towerDefend

/**
 * 该脚本为私有脚本，归WayZer所有
 * 未经许可，禁止转让给他人或者用作其他用途
 */
import coreLibrary.lib.registerVar
import coreMindustry.lib.listen
import mindustry.Vars.state
import mindustry.ai.Pathfinder
import mindustry.entities.Units
import mindustry.entities.units.AIController
import mindustry.game.EventType
import mindustry.gen.Groups
import mindustry.gen.Teamc
import mindustry.gen.Unitc
import mindustry.world.Tile
import mindustry.world.blocks.storage.CoreBlock

val control = contextScript<TDControl>()

class TowerDefendAI : AIController() {
    override fun updateTargeting() {
        updateWeapons()
    }

    override fun invalid(target: Teamc?) = when (target) {
        is CoreBlock.CoreBuild -> true
        is Tile -> target.block() is CoreBlock
        is Unitc -> with(target) {
            !isFlying && !isPlayer && floorOn() in control.floors
        }
        else -> false
    }.not()

    override fun findTarget(x: Float, y: Float, range: Float, air: Boolean, ground: Boolean): Teamc? {
        return if (unit.type.flying) unit.closestEnemyCore()
        else Units.closestEnemy(unit.team, x, y, range) { !invalid(it) }
    }

    override fun updateMovement() {
        val core = unit.closestEnemyCore() ?: return
        val range = (unit.type.range * 0.8f).coerceAtMost(80f)
        if (unit.within(core, range)) {
            target = core
            for (mount in unit.mounts) {
                if (mount.weapon.controllable && mount.weapon.bullet.collidesGround) {
                    mount.target = core
                }
            }
        } else {
            if (unit.type.flying)
                moveTo(core, unit.type.range * 0.8f)
            else
                pathfind(Pathfinder.fieldCore)
        }
        faceTarget()
    }
}

val tdMode get() = state.rules.tags.containsKey("@towerDefend")
val specialFlag = 1024.0//use for identify unit spawned from factory

fun update() {
    if (!tdMode) return
    val units = Groups.unit.filter { it.team == state.rules.waveTeam }
    units.forEach {
        if (it.flag == specialFlag) return@forEach
        if (it.controller() !is TowerDefendAI)
            it.controller(TowerDefendAI())
    }
    registerVar("TDMultiplier", "塔防难度系数", units.maxOfOrNull { it.shield }?.toInt() ?: 0)
}

onEnable { update() }
listen<EventType.PlayEvent> { update() }//修复加载存档时ai丢失
listen<EventType.WaveEvent> { update() }
listen<EventType.UnitCreateEvent> {
    if (!tdMode || it.unit.team != state.rules.waveTeam) return@listen
    it.unit.flag = specialFlag
}