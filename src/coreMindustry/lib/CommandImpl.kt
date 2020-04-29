@file:Suppress("unused")

package coreMindustry.lib

import arc.util.CommandHandler
import arc.util.Log
import cf.wayzer.script_agent.Config
import coreLibrary.lib.*
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
        if (player == null) {
            println(ColorApi.handle("$text[RESET]", ColorApi::consoleColorHandler))
        } else {
            player.sendMessage("{msg}".with("msg" to text, "player" to player), type, time)
        }
    }
}
typealias Command = ICommand<Sender>
typealias Commands = ICommands<Sender>

class RootCommands(private val mindustryHandler: CommandHandler) : Commands(null, "Root", "Root") {
    override fun addSub(name: String, command: ICommand<in Sender>, isAliases: Boolean) {
        if (command is ICommands<*>.HelpCommand) return //RootCommands don't need help
        removeSub(name)
        mindustryHandler.register(name, "[arg...]", command.description) { arg, player: Player? ->
            val sender = Sender(player)
            try {
                command.handle(sender, arg.getOrNull(0)?.split(' ') ?: emptyList(), "/$name")
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