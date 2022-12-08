package wayzer.map

import mindustry.core.ContentLoader
import mindustry.core.World
import mindustry.world.Tile
import java.awt.image.BufferedImage
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

//参考 mindustry.graphics.MinimapRenderer
object MapRenderer {
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
    fun loadColors(content: ContentLoader) {
        if (content.blocks().isEmpty) return
        val logger = thisContextScript().logger
        val img = javaClass.getResourceAsStream("/block_colors.png")?.use { ImageIO.read(it) }
            ?: return logger.warning("找不到图集 block_colors.png")
        repeat(img.width) { i ->
            val color = img.getRGB(i, 0)
            if (color != 0 && color != 255) {
                content.block(i).apply {
                    mapColor.argb8888(color)
                    squareSprite = mapColor.a > 0.5f
                    mapColor.a = 1.0f
                    hasColor = true
                }
            }
        }
        logger.info("加载方块颜色集成功")
    }

    private fun getARGB(tile: Tile?): Int {
        val rgba = getRGBA(tile)
        val a = rgba and 255
        val rgb = rgba ushr 8
        return (a shl 24) + rgb
    }

    private fun getRGBA(tile: Tile?): Int {
        return when {
            tile == null -> 0
            tile.block().minimapColor(tile) != 0 -> tile.block().minimapColor(tile)
            tile.block().synthetic() -> tile.team().color.rgba()
            tile.block().solid -> tile.block().mapColor.rgba()
            tile.overlay() != Blocks.air -> tile.overlay().mapColor.rgba()
            else -> tile.floor().mapColor.rgba()
        }
    }
}
listen<EventType.WorldLoadEvent> {
    MapRenderer.drawAll(world)
}
listen<EventType.TileChangeEvent> {
    it.tile.getLinkedTiles(MapRenderer::update)
}
listen<EventType.ContentInitEvent> {
    MapRenderer.loadColors(content)
}
onEnable {
    MapRenderer.loadColors(content)
    if (net.server())
        MapRenderer.drawAll(world)
}
registerVar("wayzer.ext.mapSnap._get", "地图快照截图接口", { MapRenderer.img })

command("saveSnap", "保存当前服务器地图截图") {
    type = CommandType.Server
    body {
        val dir = dataDirectory.child("mapSnap").apply { mkdirs() }
        val file = dir.child("mapSnap-${SimpleDateFormat("YYYYMMdd-hhmm").format(Date())}.png")
        file.write().use { ImageIO.write(MapRenderer.img, "png", it) }
        reply("[green]快照已保存到{file}".with("file" to file))
    }
}