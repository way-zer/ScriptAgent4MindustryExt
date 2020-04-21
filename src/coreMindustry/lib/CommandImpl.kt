@file:Suppress("unused")

package coreMindustry.lib

import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.script_agent.Config
import coreLibrary.lib.ICommand
import coreLibrary.lib.ICommands
import coreLibrary.lib.ISender
import coreLibrary.lib.PlaceHoldString
import mindustry.entities.type.Player

/**
 * @param player null for console
 */
@Suppress("MemberVisibilityCanBePrivate")
class Sender(override val player: Player?) : ISender<Player?> {
    override fun sendMessage(msg: PlaceHoldString) {
        sendMessage(msg, MsgType.Message)
    }

    fun sendMessage(text: PlaceHoldString, type: MsgType = MsgType.Message, time: Float = 10f) {
        player.sendMessage(text, type, time)
    }
}
typealias Command = ICommand<Sender>
typealias Commands = ICommands<Sender>

class RootCommands(private val mindustryHandler: CommandHandler) : Commands(null, "Root", "Root") {
    override fun addSub(name: String, command: ICommand<in Sender>, isAliases: Boolean) {
        if (command is ICommands<*>.HelpCommand) return //RootCommands don't need help
        removeSub(name)
        mindustryHandler.register(name, command.usage, command.description) { arg, player: Player? ->
            val sender = Sender(player)
            try {
                command.handle(sender, arg.toList(), "/$name")
            } catch (e: Throwable) {
                Log.err("Execute command $name(${command.script?.clsName}) error", e)
                sender.sendMessage("[red]error happen when execute command!".with())
            }
        }
    }

    override fun removeSub(name: String) {
        mindustryHandler.removeCommand(name)
    }
}

val clientRootCommands = RootCommands(Config.clientCommands)
val serverRootCommands = RootCommands(Config.serverCommands)