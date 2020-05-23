package web.serverList

import cf.wayzer.placehold.PlaceHoldApi
import org.eclipse.jetty.http.HttpStatus
import web.lib.serverList.PingUtil.Info
import web.lib.serverList.SharedData
import java.awt.Color
import java.awt.Font
import java.awt.image.BufferedImage
import java.io.OutputStream
import javax.imageio.ImageIO

val width by config.key(500)
val height by config.key(120)
val defaultFont = Font("黑体", 0, 16)

fun generateImg(info: Info, out: OutputStream) {
    val image = BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR)
    image.graphics.apply {
        color = Color.BLACK
        fillRect(0, 0, width, height)

        font = defaultFont
        color = Color.decode("#FFD37F")
        drawString(info.address, 15, 20)
        color = Color.decode("#BFBFBF")
        drawString(info.name, 8, 45)
        drawString(info.description, 8, 65)
        color = Color.decode("#FFD37F")
        drawString(info.players.toString(), 10, 85)
        color = Color.decode("#BFBFBF")
        drawString("位玩家在线", 30, 85)
        drawString("地图: ${info.mapName} / ${info.mode}", 8, 110)
    }.dispose()
    ImageIO.write(image, "png", out)
}

handle {
    registerVar("_develop.generateImg", "", ::generateImg)
    if (PlaceHoldApi.GlobalContext.getVar("_develop.motdImgPng") == true) return@handle
    registerVar("_develop.motdImgPng", "", true)
    get("/servers/motd.png") { ctx ->
        val address = ctx.queryParam("address") ?: let {
            ctx.status(HttpStatus.BAD_REQUEST_400)
            return@get
        }
        val info = SharedData.servers[address] ?: let {
            ctx.status(HttpStatus.BAD_REQUEST_400)
            return@get
        }
        ctx.contentType("image/png")
        val generateImg by PlaceHold.reference<(info: Info, out: OutputStream) -> Unit>("_develop.generateImg")
        generateImg(info, ctx.res.outputStream)
    }
}