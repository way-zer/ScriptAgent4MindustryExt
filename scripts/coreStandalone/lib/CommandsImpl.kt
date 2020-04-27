package coreStandalone.lib

import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.*
import coreLibrary.lib.commands.ControlCommand

open class Sender(override val player: DSLBuilder? = null) : ISender<DSLBuilder?> {
    override fun sendMessage(msg: PlaceHoldString) {
        val text = ColorApi.handle("$msg[RESET]", ColorApi::consoleColorHandler)
        println(text)
    }
}
typealias Command = ICommand<Sender>

object RootCommands : ICommands<Sender>(null, "Root", "") {
    fun handle(sender: Sender, text: String) {
        handle(sender, text.split(' ').toList(), "*")
    }

    init {
        addSub(ControlCommand { true })
    }
}