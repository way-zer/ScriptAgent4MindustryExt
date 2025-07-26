package mapScript.shared

import arc.math.Mathf
import arc.math.geom.Bresenham2
import arc.math.geom.Geometry
import arc.math.geom.Intersector
import arc.math.geom.Point2
import mindustry.content.Blocks
import mindustry.game.Rules
import mindustry.world.Tiles
import kotlin.math.ceil
import kotlin.math.sqrt

@Suppress("MemberVisibilityCanBePrivate")
open class HexedGenerator(
    val hNum: Int = 5,
    val wNum: Int = 7,
    val spacing: Int = 76,//最近中心距
    val wallWidth: Int = 3,//内六边形长
    val pathWidth: Int = 5,//相邻过道宽度
) {
    val width = ceil(((wNum - 1) / 2 * sqrt(3.0) + 1) * spacing).toInt()
    val height = hNum * spacing
    fun applyRules(rules: Rules) = rules.run {
        tags.put("hexed", "true")
        canGameOver = false
        polygonCoreProtection = true
        cleanupDeadTeams = true
    }

    val chunkCenters by lazy {
        buildList {
            val dx: Double = sqrt(3.0) * spacing / 2
            val dy = spacing / 2
            for (x in 0 until wNum) {
                for (y in 0 until hNum) {
                    //忽略最上的交错排
                    if (y == hNum - 1 && x % 2 == 1) continue
                    val cx = (spacing / 2 + x * dx).toInt()
                    val cy = dy * (1 + y * 2 + x % 2)
                    add(Point2(cx, cy))
                }
            }
        }
    }

    /**挖出六边形网格*/
    fun genHex(tiles: Tiles) {
        val d = (spacing - wallWidth) * 2 / Mathf.sqrt3
        chunkCenters.forEach {
            hexShape(it.x, it.y, d) { hx, hy ->
                tiles.getn(hx, hy).setBlock(Blocks.air)
            }
        }
    }

    /**挖出六边形网格*/
    fun genPath(tiles: Tiles) {
        chunkCenters.forEach { chunk ->
            chunkCenters.filter { it != chunk && it.dst(chunk) < spacing * 1.1 }.forEach {
                lineShape(chunk, it, pathWidth) { lx, ly ->
                    tiles.getn(lx, ly).setBlock(Blocks.air)
                }
            }
        }
    }


    //util
    fun hexShape(cx: Int, cy: Int, d: Float, body: (Int, Int) -> Unit) {
        Geometry.circle(cx, cy, width, height, ceil(d).toInt()) { x, y ->
            if (Intersector.isInsideHexagon(cx.toFloat(), cy.toFloat(), d, x.toFloat(), y.toFloat())) {
                body(x, y)
            }
        }
    }

    fun lineShape(a: Point2, b: Point2, lineWidth: Int, body: (Int, Int) -> Unit) {
        Bresenham2.line(a.x, a.y, b.x, b.y) { lx, ly ->
            Geometry.circle(lx, ly, width, height, lineWidth / 2 + 1) { x, y ->
                body(x, y)
            }
        }
    }
}