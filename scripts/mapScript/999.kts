package mapScript

import arc.math.Mathf
import arc.math.geom.Geometry
import arc.math.geom.Intersector
import arc.math.geom.Point2
import mindustry.game.Gamemode
import mindustry.game.Rules
import mindustry.game.Team
import mindustry.gen.WorldLabel
import mindustry.world.Tiles
import mindustry.world.blocks.ConstructBlock
import mindustry.world.blocks.defense.BaseShield
import mindustry.world.blocks.storage.CoreBlock
import kotlin.math.ceil
import kotlin.math.sqrt

@Suppress("DuplicatedCode")
class HexedGenerator(
    private val hNum: Int = 5,
    private val wNum: Int = 7,
    val spacing: Int = 76,//最近中心距
    val wallWidth: Int = 3,//内六边形长
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

    //util
    fun hexShape(cx: Int, cy: Int, d: Float, body: (Int, Int) -> Unit) {
        Geometry.circle(cx, cy, width, height, ceil(d).toInt()) { x, y ->
            if (Intersector.isInsideHexagon(cx.toFloat(), cy.toFloat(), d, x.toFloat(), y.toFloat())) {
                body(x, y)
            }
        }
    }
}

val generator = HexedGenerator(12, 17, spacing = 36, wallWidth = 3)
val centerRadius = 40f
val centerChunk = generator.chunkCenters.minBy { it.dst2(generator.width / 2, generator.height / 2) }
val centerChunks = generator.chunkCenters.filter { it.dst(centerChunk) <= centerRadius }
val otherChunks = generator.chunkCenters.filter { it.dst(centerChunk) > centerRadius }.mapIndexed { i, center ->
    PlayerChunk(Team.get(3 + i), center)
}
registerGenerator(
    "[gold]微泽6周年合影", "WayZer", """
            2019-2025
            
            每人一块地，自由建筑吧
            [@pvpProtect=0]""".trimIndent(),
    mode = Gamemode.sandbox,
    filter = setOf("hidden"),
    width = generator.width, height = generator.height
) {
    rules.apply {
        generator.applyRules(this)
        instantBuild = true
        infiniteResources = true
        revealedBlocks.addAll(content.blocks().select { !it.buildVisibility.visible() && it !is ConstructBlock })
        bannedBlocks.add(Blocks.coreZone)
        bannedBlocks.add(Blocks.spawn)
        bannedBlocks.addAll(
            content.blocks()
                .select { it is CoreBlock || it is BaseShield || it.name.startsWith("legacy-") || it.privileged })
        hideBannedBlocks = true
        planet = Planets.sun

        //balance
        blockHealthMultiplier = Float.MAX_VALUE
        unitHealthMultiplier = Float.MAX_VALUE
        unitDamageMultiplier = 0f
        blockDamageMultiplier = 0f

        ghostBlocks = false
        damageExplosions = false
        unitCap = 1 - (Blocks.coreShard as CoreBlock).unitCapModifier
        unitCapVariable = true
    }
    genRound("topography") { tiles ->
        tiles.eachTile {
            it.setFloor(Blocks.grass.asFloor())
            it.setBlock(Blocks.shrubs)
        }
    }
    genRound("genHex") { tiles ->
        generator.genHex(tiles)
        otherChunks.forEach {
            tiles[it.center.x, it.center.y].setBlock(Blocks.coreShard, it.team)
        }
    }
    genRound("genCenterHex") { tiles ->
        generator.hexShape(centerChunk.x, centerChunk.y, 2 * centerRadius) { x, y ->
            tiles.get(x, y).setBlock(Blocks.air)
        }
        centerChunks.forEach { tiles.get(it.x, it.y).setBlock(Blocks.coreShard, Team.sharded) }
        tiles.get(centerChunk.x, centerChunk.y).remove()
    }
}

onEnable {
    WorldLabel.create().apply {
        set(centerChunk.x * 8f, centerChunk.y * 8f)
        text = """[yellow]微泽6周年合影[]

[lightgrey]6点半开放建筑，8点正式合影[]

每人一块地，可自由建筑
中间为公共区域，点击核心可切换队伍
(性能考虑，尽量避免使用画板和处理器)
"""
    }.add()
}

data class PlayerChunk(val team: Team, val center: Point2) {
    var owner: String? = null
    val label = WorldLabel.create()!!.apply {
        set(center.x * 8f, center.y * 8f)
    }
}

val own = mutableMapOf<String, PlayerChunk>()
val justTap = mutableMapOf<String, Long>()

listen<EventType.TapEvent> { event ->
    val tile = event.tile
    val player = event.player
    if (tile.block() is CoreBlock && tile.team() != player.team()) {
        if (tile.team() == Team.sharded) {
            player.clearUnit()
            player.team(Team.sharded)
            CoreBlock.playerSpawn(tile, player)
            return@listen
        }
        val uid = PlayerData[player].takeIf { it.authed }?.id
            ?: return@listen player.sendMessage("[red]请先登录账号，再来认领领地吧")
        var chunk = otherChunks.find { it.team == tile.team() } ?: return@listen
        if (uid in own && own[uid] != chunk) {
            if (System.currentTimeMillis() - (justTap[uid] ?: 0L) < 3000) {
                player.sendMessage("[yellow]找不到自己领地了？已经给你传送到了.")
                chunk = own[uid]!!
            } else {
                justTap[uid] = System.currentTimeMillis()
                return@listen player.sendMessage("[red]你已经认领了一块地")
            }
        }
        if (chunk.owner.let { it != null && it != uid }) return@listen player.sendMessage(
            "[red]这块地已经有人认领了，另选一块吧"
        )
        chunk.owner = uid
        chunk.label.apply {
            text = player.name
            add()
        }
        own[uid] = chunk

        player.clearUnit()
        player.team(chunk.team)
        CoreBlock.playerSpawn(world.tile(chunk.center.x, chunk.center.y), player)
    }
}