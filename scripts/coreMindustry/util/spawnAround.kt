package coreMindustry.util

import arc.math.geom.Geometry
import arc.math.geom.Point2
import mindustry.Vars
import mindustry.game.Team
import mindustry.gen.Posc
import mindustry.type.UnitType

/**
 * @return 无法找到合适位置，返回null
 */
fun UnitType.spawnAround(pos: Posc, team: Team, radius: Int = 10): mindustry.gen.Unit? {
    return create(team).apply {
        set(pos)
        val valid = mutableListOf<Point2>()
        Geometry.circle(tileX(), tileY(), Vars.world.width(), Vars.world.height(), radius) { x, y ->
            if (canPass(x, y) && (!canDrown() || floorOn()?.isDeep == false))
                valid.add(Point2(x, y))
        }
        val r = valid.randomOrNull() ?: return null
        x = r.x * Vars.tilesize.toFloat()
        y = r.y * Vars.tilesize.toFloat()
        add()
    }
}