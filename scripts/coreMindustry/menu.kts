package coreMindustry

import coreLibrary.lib.util.nextEvent
import mindustry.gen.MenuChooseCallPacket
import kotlin.random.Random

data class MenuChooseEvent(
    val player: Player, val menuId: Int, val value: Int
) : Event, Event.Cancellable {
    override var cancelled: Boolean = false

    companion object : Event.Handler()
}

@Deprecated("use menuBuilder", ReplaceWith("this.menuBuilder(title,builder)"))
suspend fun <T : Any> sendMenuBuilder(
    player: Player,
    timeoutMillis: Int,
    title: String,
    msg: String,
    builder: suspend MutableList<List<Pair<String, suspend () -> T>>>.() -> Unit
): T? {
    return MenuBuilder<T>(title) {
        this.msg = msg
        buildList { builder() }.forEachIndexed { i, l ->
            if (i != 0) newRow()
            l.forEach { option(it.first, it.second) }
        }
    }.sendTo(player, timeoutMillis)
}

@Deprecated("use MenuBuilder directly", ReplaceWith("MenuBuilder(title, block)", "coreMindustry.util.MenuBuilder"))
fun <T : Any> menuBuilder(
    title: String,
    block: MenuBuilder<T>.() -> Unit
): MenuBuilder<T> = MenuBuilder(title, block)

listenPacket2ServerAsync<MenuChooseCallPacket> { con, packet ->
    con.player?.let { p ->
        MenuChooseEvent(p, packet.menuId, packet.option).emitAsync().cancelled.not()
    } ?: true
}

/**
 * 发送菜单并接收回复选项
 * 低级接口, 内部不带Timeout, 外部通常需要进行Timeout处理(否则可能无限期等待)
 * @return 原版返回值，代表选中n个选项，可能 -1 代表主动关闭
 */
suspend fun menuAsync(player: Player, title: String, msg: String, menu: Array<Array<String>>): Int {
    val id = Random.nextInt()
    Call.menu(player.con, id, title, msg, menu)
    return nextEvent<MenuChooseEvent> { it.player == player && it.menuId == id }.value
}

//TODO 接管 Commands.defaultHelpImpl