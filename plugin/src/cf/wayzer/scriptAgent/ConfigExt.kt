@file:Suppress("UnusedReceiverParameter")

package cf.wayzer.scriptAgent

import arc.util.CommandHandler
import cf.wayzer.scriptAgent.util.DSLBuilder

//Experimental
object MainScriptsHelper {
    const val defaultMain = "main/bootStrap"
    var list: List<String> = emptyList()
        private set
    private var cur = 0
    val current get() = list.getOrElse(cur) { defaultMain }

    internal fun load() {
        val params = System.getenv("SAMain")
        Config.logger.info("SAMain=${params ?: defaultMain}")
        if (params != null)
            list = params.split(";")
    }

    fun next(): String {
        if (cur < list.size) cur++
        else error("Already default main.")
        return current
    }
}

var Config.args by DSLBuilder.dataKeyWithDefault<Array<String>> { emptyArray() }
    internal set
var Config.version by DSLBuilder.lateInit<String>()
    internal set
val Config.mainScript get() = MainScriptsHelper.current
fun Config.nextMainScript() = MainScriptsHelper.next()

//Mindustry
var Config.clientCommands by DSLBuilder.lateInit<CommandHandler>()
    internal set
var Config.serverCommands by DSLBuilder.lateInit<CommandHandler>()
    internal set