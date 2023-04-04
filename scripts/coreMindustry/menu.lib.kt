package coreMindustry

import cf.wayzer.scriptAgent.contextScript
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.util.calPage
import coreLibrary.lib.util.nextEvent
import kotlinx.coroutines.withTimeoutOrNull
import mindustry.gen.Call
import mindustry.gen.Player
import kotlin.random.Random

@Suppress("unused")
open class MenuBuilder<T : Any>(private val block: suspend MenuBuilder<T>.() -> Unit = { }) {
    constructor(title: String, block: suspend MenuBuilder<T>.() -> Unit) : this(block) {
        this.title = title
    }

    @DslMarker
    annotation class MenuBuilderDsl
    object RefreshReturn : Throwable("This method should only call in callback", null, false, false)
    open class FlagOptionBuilder {
        lateinit var name: String

        @MenuBuilderDsl
        @Throws(CommandInfo.Return::class)
        open fun option(name: String) {
            this.name = name
            CommandInfo.Return()
        }

        /** This option will always call [MenuBuilder.refresh] when selected*/
        @MenuBuilderDsl
        fun refreshOption(name: String): Nothing {
            option(name)
            throw RefreshReturn
        }

        object Dummy : FlagOptionBuilder() {
            override fun option(name: String) = Unit
        }
    }

    private val menu = mutableListOf<MutableList<String>>()
    private val callback = mutableListOf<suspend () -> T>()

    @MenuBuilderDsl
    var title = ""

    @MenuBuilderDsl
    var msg = ""

    protected open suspend fun build() {
        block()
    }

    @MenuBuilderDsl
    fun newRow() = menu.add(mutableListOf())

    @MenuBuilderDsl
    fun option(name: String, body: suspend () -> T) {
        menu.last().add(name)
        callback.add(body)
    }

    @MenuBuilderDsl
    suspend fun lazyOption(body: suspend FlagOptionBuilder.() -> T) {
        val name = FlagOptionBuilder().let {
            try {
                it.body()
                error("You must call option in body")
            } catch (e: CommandInfo.Return) {
                it.name
            }
        }
        option(name) {
            FlagOptionBuilder.Dummy.body()
        }
    }

    ///api for callback
    /** mark to send refreshed menu again*/
    @MenuBuilderDsl
    fun refresh(): Nothing {
        throw RefreshReturn
    }

    private val _menuId = Random.nextInt()
    private var once = false //TODO add to constructor, and default true after bug fixed

    /** @param timeoutMillis note this is only timeout for player select, not timeout for this function (due to callback and refresh)*/
    suspend fun sendTo(player: Player, timeoutMillis: Int): T? {
        menu.clear();callback.clear()
        newRow();build()

        return withTimeoutOrNull(timeoutMillis.toLong()) {
            val options = menu.map { it.toTypedArray() }.toTypedArray()
            if (!once)
                Call.followUpMenu(player.con, _menuId, title, msg, options)
            else
                Call.menu(player.con, _menuId, title, msg, options)
            //原版返回值，代表选中n个选项，可能 -1 代表主动关闭
            val ret = utilScript.nextEvent<Menu.MenuChooseEvent> { it.player == player && it.menuId == _menuId }.value
            callback.getOrNull(ret)
        }?.let {
            try {
                it.invoke()
            } catch (e: RefreshReturn) {
                return sendTo(player, timeoutMillis)
            } catch (e: CommandInfo.Return) {
                null
            }
        }.also { close() }
    }

    fun close() {
        if (once) return
        Call.hideFollowUpMenu(_menuId)
    }

    companion object {
        private val utilScript = contextScript<Menu>()
    }
}

open class PagedMenuBuilder<T>(
    val items: List<T>,
    var selectedPage: Int = 1,
    val prePage: Int = 10,
    val itemRender: suspend PagedMenuBuilder<T>.(T) -> Unit = {},
) : MenuBuilder<Unit>() {
    protected open suspend fun renderItem(item: T) = itemRender(item)
    override suspend fun build() {
        val (page, totalPage) = calPage(selectedPage, prePage, items.size)
        items.subList((page - 1) * prePage, (page * prePage).coerceAtMost(items.size))
            .forEach { renderItem(it);newRow() }
        repeat(page * prePage - items.size) {
            option("") { refresh() };newRow()
        }
        option("<-") { selectedPage = page - 1;refresh() }
        option("$page/$totalPage") { refresh() }
        option("->") { selectedPage = page + 1;refresh() }
        newRow()
        option("关闭") {}
    }
}