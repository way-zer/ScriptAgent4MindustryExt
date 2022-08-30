package main

import arc.graphics.Color
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.type.Item
import java.awt.Image
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO

//WayZer 版权所有(禁止删除版权声明)

fun draw(x: Int, y: Int, rgb: Int) {
    fun getClosestColor(color: Color): Item {
        fun pow2(x: Float) = x * x
        return content.items().min { item ->
            with(item.color) {
                pow2(r - color.r) + pow2(g - color.g) + pow2(b - color.b)
            }
        }
    }

    fun argb8888ToColor(value: Int): Color {
        val color = Color()
        color.a = (value and -16777216 ushr 24).toFloat() / 255.0f
        color.r = (value and 16711680 ushr 16).toFloat() / 255.0f
        color.g = (value and '\uff00'.code ushr 8).toFloat() / 255.0f
        color.b = (value and 255).toFloat() / 255.0f
        return color
    }

    val color = argb8888ToColor(rgb)
    if (color.a < 0.001) return //Pass alpha pixel
    val closest = getClosestColor(color)
    //debug
//        p.sendMessage("Closest color for [#$color]$color is: [#${closest.color}]${closest.color}")
    world.tiles.getc(x, y).apply {
        if (block() != Blocks.air && block() != Blocks.sorter) return@apply
        setNet(Blocks.sorter, Team.crux, 0)
        Call.tileConfig(null, build, closest)
    }
}

fun BufferedImage.resize(maxSize: Int): BufferedImage {
    var scale = 1
    while (height / scale > maxSize || width / scale > maxSize) scale++
    val height = height / scale
    val width = width / scale
    val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
    scaled.createGraphics().apply {
        drawImage(getScaledInstance(width, height, Image.SCALE_REPLICATE), 0, 0, null)
    }
    return scaled
}

command("pixel", "绘制像素画") {
    usage = "[size=32] <url>"
    type = CommandType.Client
    permission = id.replace("/", ".")
    aliases = listOf("像素画")
    body {
        if (arg.isEmpty()) replyUsage()
        val size = arg.firstOrNull()?.toIntOrNull() ?: 32
        val url = kotlin.runCatching { URL(arg.last()) }.getOrElse {
            returnReply("[red]错误的URL: {error}".with("error" to it.message.orEmpty()))
        }

        reply("[yellow]准备开始绘制".with())
        var img = withContext(Dispatchers.IO) { ImageIO.read(url) }
        reply("[yellow]原始图片尺寸{w}x{h}".with("w" to img.width, "h" to img.height))
        img = withContext(Dispatchers.Default) { img.resize(size) }
        reply("[yellow]缩放后比例{w}x{h}".with("w" to img.width, "h" to img.height))
        var i = 0
        val p = player!!
        for (x in 1..img.width)
            for (y in 1..img.height) {
                i++
                draw(p.tileX() - img.width / 2 + x, p.tileY() + img.height / 2 - y, img.getRGB(x - 1, y - 1))
                if (i > 10) {
                    i = 0
                    yield()
                }
            }
        reply("[green]绘制完成".with())
    }
}
