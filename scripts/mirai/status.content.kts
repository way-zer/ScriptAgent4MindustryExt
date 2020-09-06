package mirai

import cf.wayzer.placehold.PlaceHoldApi
import java.awt.image.BufferedImage

subscribeGroupMessages {
    case("服务器状态") {
        @Suppress("UNCHECKED_CAST")
        val getMapSnap = PlaceHoldApi.GlobalContext.getVar("wayzer.ext.mapSnap._get") as? () -> BufferedImage
        var msg = this.message.quote() + """
                当前地图为: {map.name} 波数: {state.wave}
                服务器FPS: {fps} 内存占用(MB) {heapUse}
                当前人数: {state.playerSize} 总单位数: {state.allUnit}
            """.trimIndent().with().toString()
        getMapSnap?.let{
            msg += uploadImage(it())
        }
        reply(msg)
    }
}