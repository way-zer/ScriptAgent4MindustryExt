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

@Deprecated("use menuBuilder", ReplaceWith("this.menuBuilder(title,builder)"))
suspend fun <T> sendMenuBuilder(
    player: Player,
    timeoutMillis: Int,
    title: String,
    msg: String,
    builder: suspend MutableList<List<Pair<String, suspend () -> T>>>.() -> Unit
): T? {
    return menuBuilder<T>(title) {
        this.msg = msg
        buildList { builder() }.forEachIndexed { i, l ->
            if (i != 0) newRow()
            l.forEach { option(it.first, it.second) }
        }
    }.sendTo(player, timeoutMillis)
}

inline fun <T> menuBuilder(
    title: String,
    block: MenuBuilder<T>.() -> Unit
): MenuBuilder<T> = MenuBuilder<T>(title).apply(block)

@DslMarker
annotation class MenuBuilderDsl

inner class MenuBuilder<T>(val title: String) {
    private val menu = mutableListOf(mutableListOf<String>())
    private val callback = mutableListOf<suspend () -> T>()

    @MenuBuilderDsl
    var msg = ""

    @MenuBuilderDsl
    fun newRow() = menu.add(mutableListOf())

    @MenuBuilderDsl
    fun option(name: String, body: suspend () -> T) {
        menu.last().add(name)
        callback.add(body)
    }

    suspend fun sendTo(player: Player, timeoutMillis: Int): T? {
        val id = Random.nextInt()
        Call.menu(
            player.con, id, title, msg,
            menu.map { it.toTypedArray() }.toTypedArray()
        )
        return withTimeoutOrNull(timeoutMillis.toLong()) {
            val e = nextEvent<MenuChooseEvent> { it.player == player && it.menuId == id }
            callback.getOrNull(e.value)?.invoke()
        }
    }
}

listenPacket2Server<MenuChooseCallPacket> { con, packet ->
    con.player?.let { p ->
        MenuChooseEvent(p, packet.menuId, packet.option).emit().handled.not()
    } ?: true
}