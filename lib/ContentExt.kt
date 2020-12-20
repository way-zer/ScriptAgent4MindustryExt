package coreMindustry.lib

import arc.Core
import arc.func.Cons
import arc.util.Log
import cf.wayzer.script_agent.ISubScript
import cf.wayzer.script_agent.util.DSLBuilder.Companion.dataKeyWithDefault
import coreLibrary.lib.CommandHandler
import coreLibrary.lib.CommandInfo
import coreMindustry.lib.ContentExt.allCommands
import coreMindustry.lib.ContentExt.listener
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import mindustry.Vars
import mindustry.entities.type.Player
import kotlin.coroutines.CoroutineContext

object ContentExt {
    val ISubScript.allCommands by dataKeyWithDefault { mutableListOf<CommandInfo>() }
    val ISubScript.listener by dataKeyWithDefault { mutableListOf<Listener<*>>() }

    data class CommandInfo(
        val name: String,
        val description: String,
        val param: String = "",
        val type: CommandType = CommandType.Both,
        val runner: (arg: Array<String>, player: Player?) -> Unit
    )

    data class Listener<T : Any>(
        val script: ISubScript?,
        val cls: Class<T>,
        val handler: (T) -> Unit
    ) : Cons<T> {
        override fun get(p0: T) {
            try {
                if (script?.enabled != false) handler(p0)
            } catch (e: Exception) {
                Log.err("Error when handle event $cls in ${script?.clsName ?: "Unknown"}", e)
            }
        }
    }

    object MindustryDispatcher : CoroutineDispatcher() {
        private var mainThread: Thread? = null

        init {
            Core.app.post {
                mainThread = Thread.currentThread()
            }
        }

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            if (Thread.currentThread() == mainThread) block.run()
            else Core.app.post(block)
        }
    }
}


inline fun <reified T : Any> ISubScript.listen(noinline handler: (T) -> Unit) {
    listener.add(ContentExt.Listener(this, T::class.java, handler))
}

@Deprecated("use new command api", ReplaceWith("command(name,description,{usage=param;this.type=type},runner)"))
fun ISubScript.command(
    name: String,
    description: String,
    param: String,
    type: CommandType = CommandType.Both,
    runner: (arg: Array<String>, player: Player?) -> Unit
) {
    allCommands.add(ContentExt.CommandInfo(name, description, param, type, runner))
}

@Deprecated("use new command api", ReplaceWith("command(name,description){init();body(handler)}"))
fun ISubScript.command(name: String, description: String, init: CommandInfo.() -> Unit, handler: CommandHandler) {
    RootCommands += CommandInfo(this, name, description) {
        init()
        body(handler)
    }
}

fun ISubScript.command(name: String, description: String, init: CommandInfo.() -> Unit) {
    RootCommands += CommandInfo(this, name, description, init)
}

@Suppress("unused")
val Dispatchers.game
    get() = ContentExt.MindustryDispatcher

@Suppress("unused")
@Deprecated("请检查变量是否使用正确, Vars.player 为null", ReplaceWith("error(\"服务器中不允许使用该变量\")"), DeprecationLevel.ERROR)
val Vars.player: Player?
    get() = error("服务器中不允许使用该变量")