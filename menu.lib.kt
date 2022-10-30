package coreMindustry

import cf.wayzer.scriptAgent.contextScript
import coreLibrary.lib.CommandInfo
import kotlinx.coroutines.withTimeoutOrNull
import mindustry.gen.Player

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

    /** @param timeoutMillis note this is only timeout for player select, not timeout for this function (due to callback and refresh)*/
    suspend fun sendTo(player: Player, timeoutMillis: Int): T? {
        menu.clear();callback.clear()
        newRow();build()

        return withTimeoutOrNull(timeoutMillis.toLong()) {
            val ret = utilScript.menuAsync(
                player, title, msg,
                menu.map { it.toTypedArray() }.toTypedArray()
            )
            callback.getOrNull(ret)
        }?.let {
            try {
                it.invoke()
            } catch (e: RefreshReturn) {
                return sendTo(player, timeoutMillis)
            }
        }
    }

    companion object {
        private val utilScript = contextScript<Menu>()
    }
}