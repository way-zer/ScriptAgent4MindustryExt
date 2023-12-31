@file:Depends("wayzer/maps")
@file:Depends("coreMindustry/menu", "maps菜单")

package wayzer.ext

import coreMindustry.MenuV2
import coreMindustry.renderPaged
import wayzer.MapRegistry

val mapsPrePage by config.key(9, "/maps每页显示数")

command("maps", "列出服务器地图") {
    usage = "[page/filter] [page]"
    aliases = listOf("地图")
    body {
        val page = arg.lastOrNull()?.toIntOrNull() ?: 1
        val filter = arg.getOrNull(0) ?: "display"
        val maps = MapRegistry.searchMaps(filter)/*.sortedBy { it.id }*/
        val template = "[red]{info.id}  [green]{info.map.name}[blue] | {info.mode}"
        val player = player ?: returnReply(menu("服务器地图 By WayZer", maps, page, mapsPrePage) { info ->
            template.with("info" to info)
        })
        MenuV2(player) {
            columnPreRow = 1
            title = "服务器地图($filter)"
            msg = "SA4Mindustry By WayZer\n" +
                    "点击选项可发起投票换图"
            val url = "https://www.mindustry.top"
            option("点击打开Mindustry资源站，查看更多地图\n$url") {
                Call.openURI(player.con, url)
            }
            renderPaged(maps, page, mapsPrePage) {
                option(template.with("info" to it).toPlayer(player)) {
                    RootCommands.handleInput("vote map ${it.id}", player, "/")
                }
            }
        }.send().awaitWithTimeout()
    }
}