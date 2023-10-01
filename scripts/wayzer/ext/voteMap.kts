@file:Depends("wayzer/maps", "地图管理")
@file:Depends("wayzer/vote", "投票实现")

package wayzer.ext

import arc.util.Strings.stripColors
import arc.util.Strings.truncate
import mindustry.io.SaveIO
import wayzer.MapManager
import wayzer.MapRegistry
import wayzer.VoteService

fun VoteService.register() {
    addSubVote("换图投票", "<地图ID> [网络换图类型参数]", "map", "换图") {
        if (arg.isEmpty())
            returnReply("[red]请输入地图序号".with())
        val map = arg[0].toIntOrNull()?.let { MapRegistry.findById(it, reply) }
            ?: returnReply("[red]地图序号错误,可以通过/maps查询".with())
        val desc = "[white]地图作者: [lightgrey]${stripColors(map.map.author())}[][]\n" +
                "[white]地图简介: [lightgrey]${truncate(stripColors(map.map.description()), 100, "...")}[][]"
        start(
            player!!,
            "换图([green]{nextMap.id}[]: [green]{nextMap.map.name}[yellow]|[green]{nextMap.mode}[])"
                .with("nextMap" to map),
            extDesc = desc,
            supportSingle = true
        ) {
            broadcast("[yellow]异步加载地图中，请耐心等待".with())
            if (withContext(Dispatchers.Default) { map.map.file.exists() && !SaveIO.isSaveValid(map.map.file) })
                return@start broadcast(
                    "[red]换图失败,地图[yellow]{nextMap.name}[green](id: {nextMap.id})[red]已损坏".with(
                        "nextMap" to map.map
                    )
                )
            MapManager.loadMap(map)
            Core.app.post { // 推后,确保地图成功加载
                broadcast("[green]换图成功,当前地图[yellow]{map.name}[green](id: {map.id})".with())
            }
        }
    }
    addSubVote("回滚到某个存档(使用/slots查看)", "<存档ID>", "rollback", "load", "回档") {
        if (arg.firstOrNull()?.toIntOrNull() == null)
            returnReply("[red]请输入正确的存档编号".with())
        val map = MapManager.getSlot(arg[0].toInt())
            ?: returnReply("[red]存档不存在或存档损坏".with())
        start(player!!, "回档".with(), supportSingle = true) {
            MapManager.loadSave(map)
            broadcast("[green]回档成功".with(), quite = true)
        }
    }
}

onEnable {
    VoteService.register()
}