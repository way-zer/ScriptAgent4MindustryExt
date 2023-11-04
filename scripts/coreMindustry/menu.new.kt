package coreMindustry

import cf.wayzer.scriptAgent.contextScript
import coreLibrary.lib.CommandInfo
import coreLibrary.lib.util.nextEvent
import kotlinx.coroutines.withTimeoutOrNull
import mindustry.gen.Call
import mindustry.gen.Player
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("unused", "MemberVisibilityCanBePrivate")
open class MenuV2(
    val player: Player,
    val followup: Boolean = false,
    private val block: suspend MenuV2.() -> Unit = { }
) {
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
    private val callback = mutableListOf<suspend () -> Unit>()
    var onCancel: suspend () -> Unit = {}

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
    fun option(name: String, body: suspend () -> Unit) {
        menu.last().add(name)
        callback.add(body)
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

    suspend fun send() {
        menu.clear();callback.clear()
        newRow();build()

        val options = menu.map { it.toTypedArray() }.toTypedArray()
        if (followup)
            Call.followUpMenu(player.con, _menuId, title, msg, options)
        else
            Call.menu(player.con, _menuId, title, msg, options)
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