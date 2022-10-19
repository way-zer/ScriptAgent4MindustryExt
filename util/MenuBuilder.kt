package coreMindustry.util

import cf.wayzer.scriptAgent.contextScript
import coreLibrary.lib.CommandInfo
import coreMindustry.UtilNext
import kotlinx.coroutines.withTimeoutOrNull
import mindustry.gen.Call
import mindustry.gen.Player
import kotlin.random.Random

/** You must depends on `coreMindustry/utilNext` */
class MenuBuilder<T : Any>(val title: String, val block: suspend MenuBuilder<T>.() -> Unit) {
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
        menu.clear();newRow()
        callback.clear()
        block()

        val id = Random.nextInt()
        Call.menu(
            player.con, id, title, msg,
            menu.map { it.toTypedArray() }.toTypedArray()
        )
        return withTimeoutOrNull(timeoutMillis.toLong()) {
            val e = contextScript<UtilNext>()
                .nextEvent<UtilNext.MenuChooseEvent> { it.player == player && it.menuId == id }
            callback.getOrNull(e.value)
        }?.let {
            try {
                it.invoke()
            } catch (e: RefreshReturn) {
                return sendTo(player, timeoutMillis)
            }
        }
    }
}