@file:OptIn(ObsoleteCoroutinesApi::class)
@file:Suppress("unused")

package coreMindustry

import mindustry.gen.MenuChooseCallPacket
import mindustry.net.Administration
import kotlin.coroutines.resume
import kotlin.random.Random

interface WithHandled {
    var handled: Boolean
}

suspend inline fun <reified T : Event> nextEvent(crossinline filter: (T) -> Boolean): T = suspendCancellableCoroutine {
    lateinit var listen: Event.Listen<T>
    listen = listenTo {
        if (filter(this)) {
            if (this is WithHandled) this.handled = true
            listen.unregister()
            it.resume(this)
        }
    }
    it.invokeOnCancellation { listen.unregister() }
}


data class OnChat(val player: Player, val text: String) : Event, WithHandled {
    override var handled = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

onEnable {
    val filter = Administration.ChatFilter { p, t ->
        if (OnChat(p, t).emit().handled) null else t
    }
    netServer.admins.chatFilters.apply {
        add(filter)
        onDisable {
            remove(filter)
        }
    }
}

suspend fun nextChat(player: Player, timeoutMillis: Int): String? = withTimeoutOrNull(timeoutMillis.toLong()) {
    nextEvent<OnChat> { it.player == player }.text
}

data class MenuChooseEvent(
    val player: Player, val menuId: Int, val value: Int
) : Event, WithHandled {
    override var handled = false
    override val handler: Event.Handler get() = Companion

    companion object : Event.Handler()
}

/**
 * @param options 选项行,同行选项使用||分隔
 * @return 行,列 序号从0开始, 未选择返回-1,-1
 */
suspend fun sendMenu(
    player: Player,
    timeoutMillis: Int,
    title: String,
    msg: String,
    options: Array<String>
): Pair<Int, Int> {
    val id = Random.nextInt()
    Call.menu(
        player.con, id, title, msg,
        options.map { it.split("||").toTypedArray() }.toTypedArray()
    )
    return withTimeoutOrNull(timeoutMillis.toLong()) {
        val e = nextEvent<MenuChooseEvent> { it.player == player && it.menuId == id }
        if (e.value != -1) {
            var col = e.value
            options.forEachIndexed { row, s ->
                val len = s.split("||").size
                if (col < len) {
                    return@withTimeoutOrNull row to col
                }
                col -= len
            }
        }
        return@withTimeoutOrNull null
    } ?: (-1 to -1)
}

suspend fun <T> sendMenuBuilder(
    player: Player,
    timeoutMillis: Int,
    title: String,
    msg: String,
    builder: suspend MutableList<List<Pair<String, suspend () -> T>>>.() -> Unit
): T? {
    val list = mutableListOf<List<Pair<String, suspend () -> T>>>()
    list.builder()
    val (row, col) = sendMenu(player, timeoutMillis, title, msg, list.map { line ->
        line.joinToString("||") { it.first }
    }.toTypedArray())
    return list.getOrNull(row)?.getOrNull(col)?.second?.invoke()
}

listenPacket2Server<MenuChooseCallPacket> { con, packet ->
    con.player?.let { p ->
        MenuChooseEvent(p, packet.menuId, packet.option).emit().handled.not()
    } ?: true
}