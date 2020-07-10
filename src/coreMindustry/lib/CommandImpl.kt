@file:Suppress("unused")

package coreMindustry.lib

import arc.util.CommandHandler
import cf.wayzer.script_agent.Config
import cf.wayzer.script_agent.getContextModule
import cf.wayzer.script_agent.util.DSLBuilder
import coreLibrary.lib.*
import coreLibrary.lib.event.PermissionRequestEvent
import mindustry.entities.type.Player

class RootCommands(private val mindustryHandler: CommandHandler) : Commands() {
    override fun addSub(name: String, command: CommandInfo, isAliases: Boolean) {
        if (name == "help") return //RootCommands don't need help
        removeSub(name)
        mindustryHandler.register(name, "[arg...]", command.description) { arg, player: Player? ->
            command(CommandContext().apply{
                reply = {reply(it, MsgType.Message)}
                this.player = player
                thisCommand = command
                prefix = "/$name"
                this.arg = arg.getOrNull(0)?.split(' ')?: emptyList()
            })
        }
    }

    override fun removeSub(name: String) {
        mindustryHandler.removeCommand(name)
    }
    companion object{
        init {
            RootCommands::class.java.getContextModule()!!.apply {
                listen<PermissionRequestEvent> {
                    if(it.context.player?.isAdmin!=false)
                        it.result = true
                }
            }
        }
    }
}

/**
 * null for console or other
 */
var CommandContext.player by DSLBuilder.dataKey<Player>()
fun CommandContext.reply(text: PlaceHoldString, type: MsgType = MsgType.Message, time: Float = 10f){
    if (player == null) {
        println(ColorApi.handle("$text[RESET]", ColorApi::consoleColorHandler))
    } else {
        player.sendMessage("{msg}".with("msg" to text, "player" to player!!), type, time)
    }
}
val clientRootCommands = RootCommands(Config.clientCommands)
val serverRootCommands = RootCommands(Config.serverCommands)