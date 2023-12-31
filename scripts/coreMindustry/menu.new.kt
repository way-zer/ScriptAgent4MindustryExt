package coreMindustry

import cf.wayzer.scriptAgent.contextScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.util.calPage
import coreLibrary.lib.util.nextEvent
import kotlinx.coroutines.withTimeoutOrNull
import mindustry.gen.Call
import mindustry.gen.Player
import kotlin.properties.ReadWriteProperty
import kotlin.random.Random
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("unused", "MemberVisibilityCanBePrivate")
@MenuV2.MenuBuilderDsl
open class MenuV2(
    val player: Player,
    val followup: Boolean = false,
    private val block: suspend MenuV2.() -> Unit = { }
) {
    @DslMarker
    annotation class MenuBuilderDsl
    object RefreshReturn : Throwable("This method should only call in callback", null, false, false) {
        private fun readResolve(): Any = RefreshReturn
    }

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
    private val callback = mutableListOf<suspend () -> Unit>()
    var onCancel: suspend () -> Unit = {}

    @MenuBuilderDsl
    var title = ""

    @MenuBuilderDsl
    var msg = ""

    val sessionState = mutableMapOf<String, Any?>()
    @MenuBuilderDsl
    inline fun <reified T : Any?> stateKey(
        default: T,
        keyPrefix: String = ""
    ): DSLBuilder.NameGet<ReadWriteProperty<Any?, T>> =
        DSLBuilder.NameGet { name ->
            val key = keyPrefix + name
            return@NameGet object : ReadWriteProperty<Any?, T> {
                override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                    return sessionState.getOrPut(key) { default } as T
                }

                override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                    sessionState[key] = value
                }
            }
        }

    var columnPreRow = Int.MAX_VALUE

    protected open suspend fun build() = block()

    @MenuBuilderDsl
    inline fun column(num: Int, body: () -> Unit) {
        newRow()
        val bak = columnPreRow
        columnPreRow = num
        body()
        columnPreRow = bak
        newRow()
    }

    @MenuBuilderDsl
    fun newRow() {
        if (menu.isNotEmpty()) {
            if (menu.last().size == 0) return
            while (columnPreRow != Int.MAX_VALUE && menu.last().size < columnPreRow) {
                option("", ::refresh)
            }
        }
        menu.add(mutableListOf())
    }

    @MenuBuilderDsl
    fun option(name: String, body: suspend () -> Unit) {
        if (menu.isEmpty() || menu.last().size >= columnPreRow)
            newRow()
        menu.last().add(name)
        callback.add(body)
    }

    @MenuBuilderDsl
    fun subMenu(title: String, chooseTimeout: Duration? = 60.seconds, builder: suspend MenuV2.() -> Unit) {
        option(title) {
            this.title = title
            menu.clear();callback.clear()
            builder.invoke(this)
            var back = false
            newRow();option("返回") { back = true }

            send(rebuild = false)
            if (chooseTimeout == null) await()
            else awaitWithTimeout(chooseTimeout)

            if (back) refresh()
        }
    }

    @MenuBuilderDsl
    suspend fun lazyOption(body: suspend FlagOptionBuilder.() -> Unit) {
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

    suspend fun send(rebuild: Boolean = true): MenuV2 {
        if (rebuild) {
            menu.clear();callback.clear()
            build()
        }
        while (menu.isNotEmpty() && menu.last().isEmpty()) menu.removeLast()
        if (menu.isEmpty()) error("Menu is Empty")
        val options = menu.map { it.toTypedArray() }.toTypedArray()
        if (followup)
            Call.followUpMenu(player.con, _menuId, title, msg, options)
        else
            Call.menu(player.con, _menuId, title, msg, options)
        return this
    }

    suspend fun await() {
        val ret = utilScript.nextEvent<Menu.MenuChooseEvent> { it.player == player && it.menuId == _menuId }.value
        try {
            (callback.getOrNull(ret) ?: onCancel).invoke()
        } catch (e: RefreshReturn) {
            send();return await()
        }
    }

    /** @param chooseTimeout note this is only timeout for player select, not timeout for this function (due to callback and refresh)*/
    suspend fun awaitWithTimeout(chooseTimeout: Duration = 60.seconds) {
        val callback = withTimeoutOrNull(chooseTimeout) {
            //原版返回值，代表选中n个选项，可能 -1 代表主动关闭
            val ret =
                utilScript.nextEvent<Menu.MenuChooseEvent> { it.player == player && it.menuId == _menuId }.value
            callback.getOrNull(ret)
        } ?: onCancel
        try {
            callback()
        } catch (e: RefreshReturn) {
            send();return awaitWithTimeout(chooseTimeout)
        }
    }

    fun close() {
        if (!followup) return
        Call.hideFollowUpMenu(_menuId)
    }

    companion object {
        private val utilScript = contextScript<Menu>()
    }
}

@MenuV2.MenuBuilderDsl
inline fun <T> MenuV2.renderPaged(
    list: List<T>,
    initialPage: Int = 1,
    prePage: Int = 9,
    key: String = "",
    itemRender: (T) -> Unit
) {
    var selectedPage by stateKey(initialPage, keyPrefix = "renderPaged@$key-")
    val (page, totalPage) = calPage(selectedPage, prePage, list.size)
    repeat(prePage) {
        val i = (page - 1) * prePage + it
        if (i >= list.size) return@repeat option("", this::refresh)
        itemRender(list[i])
    }
    column(3) {
        option("<-") { selectedPage = page - 1;refresh() }
        option("$page/$totalPage", this::refresh)
        option("->") { selectedPage = page + 1;refresh() }
    }
}