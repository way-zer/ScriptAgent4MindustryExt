package mapScript.tags

import mindustry.ai.Pathfinder
import mindustry.entities.Units
import mindustry.entities.units.AIController
import mindustry.gen.Teamc
import mindustry.gen.Unitc
import mindustry.world.Tile
import mindustry.world.blocks.environment.Floor
import mindustry.world.blocks.storage.CoreBlock

class TowerDefendAI(private val floors: Set<Floor>) : AIController() {
    override fun shouldShoot(): Boolean = target != null && !invalid(target)
    override fun invalid(target: Teamc?) = when (target) {
        is CoreBlock.CoreBuild -> true
        is Tile -> target.block() is CoreBlock
        is Unitc -> with(target) {
            !isFlying && !isPlayer && floorOn() in floors
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
            if (unit.type.circleTarget) {
                this.circleAttack(range)
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