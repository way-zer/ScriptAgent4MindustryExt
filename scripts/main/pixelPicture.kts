package main

import arc.graphics.Color
import mindustry.content.Blocks
import mindustry.gen.Call
import mindustry.type.Item
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

//WayZer 版权所有(禁止删除版权声明)

val pixelDir by config.key(dataDirectory.child("pixels").file()!!, "像素画原图存放目录")
val maxSize by config.key(32, "最大像素画大小")

fun handle(file: File, body: (x: Int, y: Int, rgb: Int) -> Unit) {
    var img = ImageIO.read(file)
    fun resize(inImg: BufferedImage): BufferedImage {
        var height = img.height
        var width = img.width
        while (height > maxSize || width > maxSize) {
            height /= 2
            width /= 2
        }
        val scaled = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        scaled.createGraphics().apply {
            drawImage(inImg.getScaledInstance(width, height, Image.SCALE_REPLICATE), 0, 0, null)
        }
        return scaled
    }
    img = resize(img)
//    pixelDir.resolve(file.nameWithoutExtension + "min.png").outputStream().use { ImageIO.write(img, "png", it) }
    for (x in 1..img.width)
        for (y in 1..img.height)
            body(x, y, img.getRGB(x - 1, y - 1))
}

fun draw(p: Player, file: File) {
    fun getClosestColor(color: Color): Item {
        fun pow2(x: Float) = x * x
        return content.items().min { item ->
            with(item.color) {
                pow2(r - color.r) + pow2(g - color.g) + pow2(b - color.b)
            }
        }
    }

    fun place(x: Int, y: Int, item: Item) {
        val tile = world.tile(x, y) ?: return
        if (tile.block() != Blocks.air) return
        Call.constructFinish(tile, Blocks.sorter, p.unit(), 0, p.team(), true)
        Call.tileConfig(p, tile.build, item.id.toInt())
    }
    //debug
//    content.items().forEachIndexed { index, item ->
//        p.sendMessage("[#${item.color}]${item.name}")
//        place(p.tileX() + index + 1, p.tileY() + 1, item)
//    }

    fun argb8888ToColor(value: Int): Color {
        val color = Color()
        color.a = (value and -16777216 ushr 24).toFloat() / 255.0f
        color.r = (value and 16711680 ushr 16).toFloat() / 255.0f
        color.g = (value and '\uff00'.toInt() ushr 8).toFloat() / 255.0f
        color.b = (value and 255).toFloat() / 255.0f
        return color
    }
    handle(file) { x, y, rgba ->
        val color = argb8888ToColor(rgba)
        if (color.a < 0.001) return@handle //Pass alpha pixel
        val closest = getClosestColor(color)
        //debug
//        p.sendMessage("Closest color for [#$color]$color is: [#${closest.color}]${closest.color}")
        place(p.tileX() + x, p.tileY() - y, closest)
    }
}

command("pixel", "绘制像素画") {
    usage = "[fileName]"
    type = CommandType.Client
    permission = id.replace("/", ".")
    aliases = listOf("像素画")
    body {
        if (arg.isEmpty()) {
            val list = (pixelDir.listFiles() ?: emptyArray()).joinToString("\n") { it.name }
            reply(
                """
            ==== 可用图片 ====
            {list}
        """.trimIndent().with("list" to list)
            )
        } else {
            val file = pixelDir.resolve(arg[0])
            if (!file.exists()) returnReply("[red]找不到对应文件".with())
            reply("[yellow]准备开始绘制".with())
            launch(Dispatchers.game) {
                draw(player!!, file)
                reply("[green]绘制完成".with())
            }
        }
    }
}
