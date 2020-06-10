package coreStandalone.lib

import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.*

open class Sender(override val player: DSLBuilder? = null) : ISender<DSLBuilder?> {
    override fun sendMessage(msg: PlaceHoldString) {
        val text = ColorApi.handle("$msg[RESET]", ColorApi::consoleColorHandler)
        println(text)
    }

    override fun hasPermission(node: String): Boolean {
        return true
    }
}
typealias Command = ICommand<Sender>

object RootCommands : ICommands<Sender>(null, "Root", "") {
    fun handle(sender: Sender, text: String) {
        try {
            handle(sender, text.split(' ').toList(), "*")
        } catch (e: Throwable) {
            sender.sendMessage("exception happened:{e}".with("e" to e))
        }
    }
}