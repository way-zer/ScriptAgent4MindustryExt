package wayzer.ext

import arc.util.Log
import cf.wayzer.script_agent.Config
import mindustry.core.ContentLoader
import mindustry.core.World
import mindustry.game.EventType
import mindustry.io.MapIO
import mindustry.world.Tile
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

//参考 mindustry.graphics.MinimapRenderer
object MapRenderer {
    init {
        loadColors(content)
    }

    var img: BufferedImage? = null
    fun drawAll(world: World) {
        img = BufferedImage(world.width(), world.height(), BufferedImage.TYPE_INT_ARGB).apply {
            repeat(world.width()) { x ->
                repeat(world.height()) { y ->
                    setRGB(x, height - 1 - y, getARGB(world.tile(x, y)))
                }
            }
        }
    }

    fun update(tile: Tile) {
        img?.apply {
            setRGB(tile.x.toInt(), height - 1 - tile.y, getARGB(tile))
        }
    }

    //参考 mindustry.core.ContentLoader.loadColors
    private fun loadColors(content: ContentLoader) {
        if (content.blocks().isEmpty) return
        val img = Config.getModuleDir("wayzer").resolve("res/block_colors.png")
                .takeIf { it.exists() && it.canRead() }?.inputStream()?.use { ImageIO.read(it) }
        if (img == null) Log.warn("[wayzer/ext/mapSnap]找不到图集res/block_colors.png")
        fun ARGBtoRGBA(argb: Int): Int {
            val a = argb ushr 24
            val rgb = argb and 0xff_ff_ff
            return (rgb shl 8) + a
        }
        img?.apply {
            repeat(width) { i ->
                val c = ARGBtoRGBA(getRGB(i, 0))
                if (c != 0) {
                    content.block(i).mapColor.set(c)
                }
            }
            Log.info("[wayzer/ext/mapSnap]加载方块颜色集成功")
        }
    }

    private fun getARGB(tile: Tile?): Int {
        val rgba = getRGBA(tile)
        val a = rgba and 255
        val rgb = rgba ushr 8
        return (a shl 24) + rgb
    }

    private fun getRGBA(tile: Tile?): Int {
        if (tile == null) return 0
        val blockColor = tile.block().minimapColor(tile)
        if (blockColor != 0) return blockColor
        return MapIO.colorFor(tile.floor(), tile.block(), tile.overlay(), tile.team())
    }
}
listen<EventType.WorldLoadEvent> {
    MapRenderer.drawAll(world)
}
listen<EventType.BuildinghangeEvent> {
    launch {
        MapRenderer.update(it.tile)
    }
}
onEnable {
    if (net.server())
        MapRenderer.drawAll(world)
}
registerVar("wayzer.ext.mapSnap._get", "地图快照截图接口", { MapRenderer.img })

command("saveSnap", "保存当前服务器地图截图", { type = CommandType.Server }) {
    val dir = dataDirectory.child("mapSnap").apply { mkdirs() }
    val file = dir.child("mapSnap-${SimpleDateFormat("YYYYMMdd-hhmm").format(Date())}.png")
    file.write().use { ImageIO.write(MapRenderer.img, "png", it) }
    reply("[green]快照已保存到{file}".with("file" to file))
}