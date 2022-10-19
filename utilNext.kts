@file:Import("@coreMindustry/util/MenuBuilder.kt", sourceFile = true)
@file:Suppress("unused")

package coreMindustry

import coreMindustry.util.MenuBuilder
import mindustry.gen.MenuChooseCallPacket
import mindustry.gen.SendChatMessageCallPacket
import kotlin.coroutines.resume

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

listenPacket2ServerAsync<SendChatMessageCallPacket> { con, p ->
    con.player?.let {
        OnChat(it, p.message).emitAsync().handled.not()
    } ?: true
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
        MenuChooseEvent(p, packet.menuId, packet.option).emitAsync().handled.not()
    } ?: true
}